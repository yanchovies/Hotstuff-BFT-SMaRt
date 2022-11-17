package bftsmart.consensus.roles;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.consensus.Consensus;
import bftsmart.consensus.Epoch;
import bftsmart.consensus.messages.*;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.util.thresholdsig.Thresig;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.TOMLayer;
import com.sun.org.apache.bcel.internal.generic.BREAKPOINT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class Primary {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private int me; // This replica ID
    private MessageFactory factory; // Factory for PaW messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private TOMLayer tomLayer; // TOM layer
    private ServerViewController controller;
    //private Cipher cipher;
    private byte[] value = null;
    private BigInteger n;
    private BigInteger e;
    private int cid = -1;
    private boolean alreadyPreCommit = false;
    private boolean alreadyCommit = false;
    private boolean alreadyDecide = false;
    private List<SigShareMessage> prepareVotesigs = new ArrayList<>();
    private List<SigShareMessage> preCommitSigs = new ArrayList<>();
    private List<SigShareMessage> commitSigs = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();
    private int count = 0;
    public Primary(ServerCommunicationSystem communication, MessageFactory factory, ServerViewController controller) {
        this.communication = communication;
        this.factory = factory;
        this.controller = controller;
    }

    public void deliver(SigShareMessage msg){
        Consensus consensus = tomLayer.execManager.getConsensus(msg.getNumber());
        Epoch epoch = consensus.getEpoch(msg.getEpoch(), controller);
        lock.lock();
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
        }
        lock.unlock();
    }

    /**
     *  handle prepareVote
     */
    public void prepareVoteReceived(SigShareMessage msg,Epoch epoch){
        if(cid == msg.getNumber()){
            prepareVotesigs.add(msg);
            if(prepareVotesigs.size()==controller.getCurrentViewN()){//or (size > controller.getQuoRum())
                SigShareMessage[] sigsArray = prepareVotesigs.toArray(new SigShareMessage[0]);
                boolean verify = Thresig.verify(value, sigsArray, prepareVotesigs.size(), controller.getCurrentViewN(), this.n, this.e);
                if(verify&&!alreadyPreCommit){
                    logger.debug("Validation succeeded!");
                    communication.send(this.controller.getCurrentViewAcceptors(), factory.createPreCommit(epoch.getConsensus().getId(),0,value));
                    alreadyPreCommit = true;
                }else if(!verify){
                    logger.debug("verification failed!!!   need to view change...");
                } else {
                    logger.debug("enough signatures have been received!!!");
                }
            }
        } else {
            logger.debug("out of date message!!!");
        }
    }

    /**
     *  handle preCommitVote
     */
    public void preCommitVoteReceived(SigShareMessage msg,Epoch epoch){
        if(cid == msg.getNumber()){
            preCommitSigs.add(msg);
            if(preCommitSigs.size()==controller.getCurrentViewN()){//or (size > controller.getQuoRum())
                SigShareMessage[] sigsArray = preCommitSigs.toArray(new SigShareMessage[0]);
                boolean verify = Thresig.verify(value, sigsArray, preCommitSigs.size(), controller.getCurrentViewN(), this.n, this.e);
                if(verify&&!alreadyCommit){
                    logger.debug("Validation succeeded!");
                    communication.send(this.controller.getCurrentViewAcceptors(), factory.createCommit(epoch.getConsensus().getId(),0,value));
                    alreadyCommit = true;
                }else if(!verify){
                    logger.debug("verification failed!!!   need to view change...");
                } else {
                    logger.debug("enough signatures have been received!!!");
                }
            }
        } else {
            logger.debug("out of date message!!!");
        }
    }

    /**
     * handle commitVote
     */
    public void commitVoteReceived(SigShareMessage msg,Epoch epoch){
        if(cid == msg.getNumber()){
            commitSigs.add(msg);
            if(commitSigs.size()==controller.getCurrentViewN()){//or (size > controller.getQuoRum())
                SigShareMessage[] sigsArray = commitSigs.toArray(new SigShareMessage[0]);
                boolean verify = Thresig.verify(value, sigsArray, commitSigs.size(), controller.getCurrentViewN(), this.n, this.e);
                if(verify&&!alreadyDecide){
                    logger.debug("Validation succeeded!");
                    communication.send(this.controller.getCurrentViewAcceptors(), factory.createDecide(epoch.getConsensus().getId(),0,value));
                    alreadyDecide = true;
                }else if(!verify){
                    logger.debug("verification failed!!!   need to view change...");
                } else {
                    logger.debug("enough signatures have been received!!!");
                }
                reset();
            }
        } else {
            logger.debug("out of date message!!!");
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
        this.cid = cid;
        this.value = value;
        ConsensusMessage msg = factory.createPrepare(cid, 0, value);
        reset();
        communication.send(this.controller.getCurrentViewAcceptors(), msg);
//        if(msg.getNumber()%3==0) {
            tomLayer.imAmNotTheLeader();
//        }
    }
    public void newViewReceived(NewViewMessage newViewMessage){
        count++;
        if (count==controller.getCurrentViewN()&&newViewMessage.getNewLeaderId()==controller.getStaticConf().getProcessId()){//or (count > controller.getQuoRum())
            tomLayer.imAmTheLeader();
        }
    }

    public void sendThresholdSigKeys(){
        KeyShareMessage[] keyShareMessages = Thresig.generateKeys(controller.getCurrentViewN() - controller.getCurrentViewF(), controller.getCurrentViewN());
        this.n = keyShareMessages[0].getN();
        this.e = keyShareMessages[0].getE();//public key
        for(int i=0;i<controller.getCurrentViewN();i++){
            keyShareMessages[i].setSender(controller.getStaticConf().getProcessId());
            tomLayer.getCommunication().send(new int[]{i},keyShareMessages[i]);
        }
    }
    public void setEandN(byte[] value,BigInteger e,BigInteger n){
        this.value = value;
        this.e = e;
        this.n = n;
    }
    public void verifyKey(KeyShareMessage keyShare){
        BigInteger secert = keyShare.getSecret();
        int id = keyShare.getId();
        boolean verifyKey = Thresig.verifyKey(secert, id);
    }

    public void reset(){
        count = 0;
        preCommitSigs.clear();
        prepareVotesigs.clear();
        commitSigs.clear();
        alreadyCommit = false;
        alreadyDecide = false;
        alreadyPreCommit = false;
    }
}
