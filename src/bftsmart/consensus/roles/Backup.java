package bftsmart.consensus.roles;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.consensus.Consensus;
import bftsmart.consensus.Epoch;
import bftsmart.consensus.messages.*;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.util.thresholdsig.Thresig;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.TOMLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @Author Moonk
 * @Date 2022/4/21
 */
public class Backup {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private int me;
    private ExecutionManager executionManager;
    private MessageFactory factory; // Factory for PaW messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private TOMLayer tomLayer; // TOM layer
    private ServerViewController controller;
    private BigInteger secret;
    private int keyId;
    private BigInteger verifier;
    private BigInteger groupVerifier;
    private BigInteger n;
    private int l;
    private int cid;
    private BigInteger e;
    public ReentrantLock lock = new ReentrantLock();

    public Backup(ServerCommunicationSystem communication, MessageFactory factory, ServerViewController controller) {
        this.communication = communication;
        this.factory = factory;
        this.controller = controller;
        this.me = controller.getStaticConf().getProcessId();
    }
    public void setExecutionManager(ExecutionManager executionManager) {
        this.executionManager = executionManager;
    }
    public void setTOMLayer(TOMLayer tomLayer) {
        this.tomLayer = tomLayer;
    }
    public void deliver(ConsensusMessage msg){
        if (executionManager.checkLimits(msg)) {
            logger.debug("Processing paxos msg with id " + msg.getNumber());
            processMessage(msg);
        } else {
            logger.debug("Out of context msg with id " + msg.getNumber());
            tomLayer.processOutOfContext();
        }

    }
    public void processMessage(ConsensusMessage msg){
        Consensus consensus = executionManager.getConsensus(msg.getNumber());
        lock.lock();
        Epoch epoch = consensus.getEpoch(msg.getEpoch(), controller);
        switch (msg.getType()){
            case MessageFactory.PREPARE:{
                prepareReceived(epoch,msg);
            }break;
            case MessageFactory.PRECOMMIT:{
                preCommitReceived(epoch,msg);
            }break;
            case MessageFactory.COMMIT:{
                commitReceived(epoch,msg);
            }break;
            case MessageFactory.DECIDE:{
                decideReceived(epoch,msg);
            }break;
        }
        lock.unlock();
    }

    private void prepareReceived(Epoch epoch, ConsensusMessage msg) {
        if(epoch.propValue == null) {
            epoch.propValue = msg.getValue();
            epoch.propValueHash = tomLayer.computeHash(msg.getValue());
            epoch.getConsensus().addWritten(msg.getValue());
            epoch.deserializedPropValue = tomLayer.checkProposedValue(msg.getValue(), true);
            epoch.getConsensus().getDecision().firstMessageProposed = epoch.deserializedPropValue[0];
        }
        cid = msg.getNumber();

        SigShareMessage sign = Thresig.sign(msg.getValue(), keyId, n, groupVerifier, verifier, secret, l,cid,epoch.getTimestamp(),MessageFactory.PREPAREVOTE);
        sign.setSender(me);
        communication.send(new int[]{executionManager.getCurrentLeader()}, sign);
//        executionManager.processOutOfContext(epoch.getConsensus());
    }
    private void preCommitReceived(Epoch epoch, ConsensusMessage msg) {
        cid = msg.getNumber();
        SigShareMessage sign = Thresig.sign(msg.getValue(), keyId, n, groupVerifier, verifier, secret, l,cid,epoch.getTimestamp(),MessageFactory.PRECOMMITVOTE);
        sign.setSender(me);
        communication.send(new int[]{executionManager.getCurrentLeader()}, sign);
//        executionManager.processOutOfContext(epoch.getConsensus());
    }

    private void commitReceived(Epoch epoch, ConsensusMessage msg) {
        cid = msg.getNumber();
        SigShareMessage sign = Thresig.sign(msg.getValue(), keyId, n, groupVerifier, verifier, secret, l,cid,epoch.getTimestamp(),MessageFactory.COMMITVOTE);
        sign.setSender(me);
        communication.send(new int[]{executionManager.getCurrentLeader()}, sign);
//        executionManager.processOutOfContext(epoch.getConsensus());
    }

    private void decideReceived(Epoch epoch, ConsensusMessage msg) {
//        if(msg.getNumber()%3==0){   // the same to Primary : 115         3 rounds of consensus to change leaders
            int newLeaderId = tomLayer.getSynchronizer().getLCManager().getNewLeader();
            NewViewMessage newViewMessage = new NewViewMessage(newLeaderId);
            newViewMessage.setSender(me);
            executionManager.setNewLeader(newLeaderId);
            System.out.println("#####################################################################################new leader id:"+newLeaderId);
            communication.send(new int[]{newLeaderId},newViewMessage);
//        }
        decide(epoch);
    }
    private void decide(Epoch epoch) {
        epoch.getConsensus().decided(epoch, true);
    }

    public void setKeyShare(KeyShareMessage keyShare){
        this.secret = keyShare.getSecret();
        this.keyId = keyShare.getId();
        this.groupVerifier = keyShare.getGroupVerifier();
        this.n = keyShare.getN();
        this.e = keyShare.getE();
        this.verifier = keyShare.getVerifier();
        this.l = keyShare.getL();
//        this.sendReceiveKey();
    }
    public void sendReceiveKey(){
        communication.send(new int[]{executionManager.getCurrentLeader()},new KeyShareMessage(keyId,secret));
    }

}
