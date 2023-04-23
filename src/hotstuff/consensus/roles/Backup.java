package hotstuff.consensus.roles;

import hotstuff.communication.ServerCommunicationSystem;
import hotstuff.consensus.Consensus;
import hotstuff.consensus.Epoch;
import hotstuff.consensus.messages.*;
import hotstuff.reconfiguration.ServerViewController;
import hotstuff.tom.util.thresholdsig.Thresig;
import hotstuff.tom.core.ExecutionManager;
import hotstuff.tom.core.TOMLayer;
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
        consensus.lock.lock();
        Epoch epoch = consensus.getEpoch(msg.getEpoch(), controller);
        switch (msg.getType()){
            case MessageFactory.KEYSHARE:{
                KeyShareMessage keyShareMessage = (KeyShareMessage) msg;
                if (keyShareMessage.getSender() == executionManager.getCurrentLeader()){
                    setKeyShare((KeyShareMessage) msg);
                }
            }break;
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
        consensus.lock.unlock();
    }

    private void prepareReceived(Epoch epoch, ConsensusMessage msg) {

        if(epoch.propValue == null) {
            epoch.propValue = msg.getValue();
            epoch.propValueHash = tomLayer.computeHash(msg.getValue());
            epoch.getConsensus().addWritten(msg.getValue());
            epoch.deserializedPropValue = tomLayer.checkProposedValue(msg.getValue(), true);
            epoch.getConsensus().getDecision().firstMessageProposed = epoch.deserializedPropValue[0];
        }
        if(this.keyId!=0){
            SigShareMessage sign = Thresig.sign(epoch.propValueHash, keyId, n, groupVerifier, verifier, secret, l,msg.getNumber(),epoch.getTimestamp(),MessageFactory.PREPAREVOTE,controller.getStaticConf().getProcessId());
            communication.send(new int[]{executionManager.getCurrentLeader()}, sign);
        }
        executionManager.processOutOfContext(epoch.getConsensus());
    }
    private void preCommitReceived(Epoch epoch, ConsensusMessage msg) {
        if(this.keyId!=0) {
            SigShareMessage sign = Thresig.sign(epoch.propValueHash, keyId, n, groupVerifier, verifier, secret, l, msg.getNumber(), epoch.getTimestamp(), MessageFactory.PRECOMMITVOTE, controller.getStaticConf().getProcessId());
            communication.send(new int[]{executionManager.getCurrentLeader()}, sign);
            executionManager.processOutOfContext(epoch.getConsensus());
        }
    }

    private void commitReceived(Epoch epoch, ConsensusMessage msg) {

        if(this.keyId!=0) {
            SigShareMessage sign = Thresig.sign(epoch.propValueHash, keyId, n, groupVerifier, verifier, secret, l, msg.getNumber(), epoch.getTimestamp(), MessageFactory.COMMITVOTE, controller.getStaticConf().getProcessId());
            communication.send(new int[]{executionManager.getCurrentLeader()}, sign);
            executionManager.processOutOfContext(epoch.getConsensus());
        }

    }

    private void decideReceived(Epoch epoch, ConsensusMessage msg) {
        decide(epoch);
        executionManager.processOutOfContext(epoch.getConsensus());
        int newLeaderId = 0;//tomLayer.getSynchronizer().getLCManager().getNewLeader();
        NewViewMessage newViewMessage = factory.createNewLeader(newLeaderId,msg.getNumber(),epoch.getTimestamp());
        newViewMessage.setSender(me);
        executionManager.setNewLeader(newLeaderId);
        communication.send(new int[]{newLeaderId},newViewMessage);
    }
    private void decide(Epoch epoch) {
        epoch.getConsensus().decided(epoch, true);
    }

    public void setKeyShare(KeyShareMessage keyShare){
        this.secret = keyShare.getSecret();
        this.keyId = keyShare.getId();
        this.groupVerifier = keyShare.getGroupVerifier();
        this.n = keyShare.getN();
        this.verifier = keyShare.getVerifier();
        this.l = keyShare.getL();
    }

}
