package hotstuff.consensus.roles;

import hotstuff.communication.ServerCommunicationSystem;
import hotstuff.consensus.Consensus;
import hotstuff.consensus.Epoch;
import hotstuff.consensus.messages.*;
import hotstuff.reconfiguration.ServerViewController;
import hotstuff.tom.util.ClassUtil;
import hotstuff.tom.util.thresholdsig.Thresig;
import hotstuff.tom.core.TOMLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Primary {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private MessageFactory factory; // Factory for PaW messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private TOMLayer tomLayer; // TOM layer
    private ServerViewController controller;
    private final int me; // This replica ID
    //private Cipher cipher;
    private byte[] value = null;
    private BigInteger n;
    private BigInteger e;

    public Primary(ServerCommunicationSystem communication, MessageFactory factory, ServerViewController controller) {
        this.communication = communication;
        this.factory = factory;
        this.controller = controller;
        this.me = controller.getStaticConf().getProcessId();
    }

    public void deliver(ConsensusMessage msg){
        processMessage(msg);
    }


    public void processMessage(ConsensusMessage msg) {
        Consensus consensus = tomLayer.execManager.getConsensus(msg.getNumber());
        Epoch epoch = consensus.getEpoch(msg.getEpoch(), controller);
        switch (msg.getType()){
            case MessageFactory.PREPAREVOTE:{
                prepareVoteReceived(msg,epoch);
            }break;
            case MessageFactory.PRECOMMITVOTE:{
                preCommitVoteReceived(msg,epoch);
            }break;
            case MessageFactory.COMMITVOTE:{
                commitVoteReceived(msg,epoch);
            }break;
            case MessageFactory.NEWVIEW:{
                newViewReceived(msg);
            }break;
        }
    }

    /**
     *  handle prepareVote
     */
    public void prepareVoteReceived(ConsensusMessage consensusMessage,Epoch epoch){
        SigShareMessage msg = (SigShareMessage) consensusMessage;
        epoch.setPrepareVotesigs(msg);
        int count = epoch.countPrepareSignatures(value, controller.getCurrentViewN(), this.n);
        if (count >controller.getQuorum()  && !epoch.PreCommit) {//controller.getQuorum()
            SigShareMessage[] sigsArray = epoch.getPrepareVotesigs(count);
            boolean verify = Thresig.verify(value, sigsArray, count, controller.getCurrentViewN(), this.n, this.e);
            if ((verify) ) {
                epoch.setThresholdSigPK(this.n, this.e);
                logger.debug("Validation succeeded!");
                    communication.send(this.controller.getCurrentViewAcceptors(), factory.createPreCommit(epoch.getConsensus().getId(),0,value));
                epoch.PreCommit = true;
            } else if (!tomLayer.isChangingLeader()) {
                logger.debug("verification failed!   need to view change...");
            } else {
                logger.debug("enough signatures have been received!");
            }
        }
    }

    /**
     *  handle preCommitVote
     */
    public void preCommitVoteReceived(ConsensusMessage consensusMessage,Epoch epoch){
        SigShareMessage msg = (SigShareMessage) consensusMessage;
        epoch.setPreCommitSigs(msg);
        int count = epoch.countPreCommitSignatures(value, controller.getCurrentViewN(), this.n);
        if (count >controller.getQuorum()&& !epoch.Commit) {
            SigShareMessage[] sigsArray = epoch.getPreCommitSigs(count);
            boolean verify = Thresig.verify(value, sigsArray, count, controller.getCurrentViewN(), this.n, this.e);
            if ((verify)) {
                epoch.setThresholdSigPK(this.n, this.e);
                logger.debug("Validation succeeded!");
                communication.send(this.controller.getCurrentViewAcceptors(), factory.createCommit(epoch.getConsensus().getId(),0,value));
                epoch.Commit = true;
            } else if (!tomLayer.isChangingLeader()) {
                logger.debug("verification failed!   need to view change...");
            } else {
                logger.debug("enough signatures have been received!");
            }
        }
    }

    /**
     * handle commitVote
     */
    public void commitVoteReceived(ConsensusMessage consensusMessage,Epoch epoch){
        SigShareMessage msg = (SigShareMessage) consensusMessage;
        epoch.setCommitSigs(msg);
        int count = epoch.countCommitSignatures(value, controller.getCurrentViewN(), this.n);
        if (count >controller.getQuorum() && !epoch.Decide) {
            SigShareMessage[] sigsArray = epoch.getCommitSigs(count);
            boolean verify = Thresig.verify(value, sigsArray, count, controller.getCurrentViewN(), this.n, this.e);
            if ((verify)) {
                epoch.setThresholdSigPK(this.n, this.e);
                logger.debug("Validation succeeded!");
                communication.send(this.controller.getCurrentViewAcceptors(), factory.createDecide(epoch.getConsensus().getId(),0,value));
                epoch.Decide = true;
            } else if (!tomLayer.isChangingLeader()) {
                logger.debug("verification failed!   need to view change...");
            } else {
                logger.debug("enough signatures have been received!");
            }
        }
    }

    public void setTOMLayer(TOMLayer tomLayer){
        this.tomLayer = tomLayer;
    }

    /**
     * start Consensus
     * @param cid
     * @param value
     */
    public void startConsensus(int cid, byte[] value) {
        this.value = tomLayer.computeHash(value);
        ConsensusMessage msg = factory.createPrepare(cid, 0, value);
        communication.send(this.controller.getCurrentViewAcceptors(), msg);
    }
    public void newViewReceived(ConsensusMessage consensusMessage){
        NewViewMessage newViewMessage = (NewViewMessage) consensusMessage;
        Consensus consensus = tomLayer.execManager.getConsensus(newViewMessage.getNumber());
        Epoch epoch = consensus.getEpoch(newViewMessage.getEpoch(), controller);
        consensus.lock.lock();
        epoch.setLeaderCount();
        if (epoch.getLeaderCount() >controller.getQuorum()  && newViewMessage.getNewLeaderId()==controller.getStaticConf().getProcessId()){//or controller.getCurrentViewN()(count > controller.getQuoRum())
                if(epoch.getLeader()==-1){
                    tomLayer.imAmTheLeader();
                    epoch.setLeader(controller.getStaticConf().getProcessId());
                }else if(consensusMessage.getSender()==controller.getStaticConf().getProcessId()){
                    tomLayer.execManager.setNewLeader(controller.getStaticConf().getProcessId());
                    tomLayer.imAmTheLeader();
                }
        }

        consensus.lock.unlock();
    }

    public void sendThresholdSigKeys(int execId){
        KeyShareMessage[] keyShareMessages = Thresig.generateKeys(controller.getCurrentViewN() - controller.getCurrentViewF(), controller.getCurrentViewN(), me,execId);
        this.n = keyShareMessages[0].getN();
        this.e = keyShareMessages[0].getE();//public key
        for(int i=0;i<controller.getCurrentViewN();i++){
            tomLayer.getCommunication().send(new int[]{i},keyShareMessages[i]);
        }
    }
    public void setEandN(byte[] value,BigInteger e,BigInteger n){
        this.value = tomLayer.computeHash(value);
        this.e = e;
        this.n = n;
    }

}
