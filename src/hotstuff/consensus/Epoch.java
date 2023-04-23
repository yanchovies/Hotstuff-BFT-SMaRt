/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package hotstuff.consensus;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import hotstuff.consensus.messages.ConsensusMessage;

import hotstuff.consensus.messages.SigShareMessage;
import hotstuff.reconfiguration.ServerViewController;
import hotstuff.reconfiguration.views.View;
import hotstuff.tom.core.messages.TOMMessage;
import hotstuff.tom.util.thresholdsig.Thresig;


/**
 * This class stands for a consensus epoch, as described in
 * Cachin's 'Yet Another Visit to Paxos' (April 2011)
 */
public class Epoch implements Serializable {

    private static final long serialVersionUID = -2891450035863688295L;
    private final transient Consensus consensus; // Consensus where the epoch belongs to

    private final int timestamp; // Epochs's timestamp
    private final int me; // Process ID
    private boolean alreadyRemoved = false; // indicates if this epoch was removed from its consensus
    public byte[] propValue = null; // proposed value
    public SigShareMessage[] prepareVotesigs = null;
    public SigShareMessage[] preCommitSigs = null;
    public SigShareMessage[] commitSigs = null;
    private boolean[] prepareVotesigsSetted = null;
    private boolean[] preCommitSigsSetted = null;
    private boolean[] commitSigsSetted = null;
    public TOMMessage[] deserializedPropValue = null; //utility var
    public byte[] propValueHash = null; // proposed value hash
    public HashSet<SigShareMessage> proof; // threshold signature proof
    private BigInteger n = null;   //threshold signature public key
    private BigInteger e = null;   //threshold signature public key
    private View lastView = null;
    private int leaderCount = 0;
    private int leader = -1;

    public boolean PreCommit = false;
    public boolean Commit = false;
    public boolean Decide = false;

    private ServerViewController controller;

    /**
     * Creates a new instance of Epoch for acceptors
     * @param controller
     * @param parent Consensus to which this epoch belongs
     * @param timestamp Timestamp of the epoch
     */
    public Epoch(ServerViewController controller, Consensus parent, int timestamp) {
        this.consensus = parent;
        this.timestamp = timestamp;
        this.controller = controller;
        this.proof = new HashSet<>();
        //ExecutionManager manager = consensus.getManager();

        this.lastView = controller.getCurrentView();
        this.me = controller.getStaticConf().getProcessId();
        int n = controller.getCurrentViewN();
        this.prepareVotesigs = new SigShareMessage[n];
        this.preCommitSigs = new SigShareMessage[n];
        this.commitSigs = new SigShareMessage[n];
        this.prepareVotesigsSetted = new boolean[n];
        this.preCommitSigsSetted = new boolean[n];
        this.commitSigsSetted = new boolean[n];
        Arrays.fill(prepareVotesigsSetted,false);
        Arrays.fill(preCommitSigsSetted,false);
        Arrays.fill(commitSigsSetted,false);
    }

    public int getLeaderCount() {
        return leaderCount;
    }

    public void setLeaderCount() {
        this.leaderCount += 1;
    }

    /**
     * Set this epoch as removed from its consensus instance
     */
    public void setRemoved() {
        this.alreadyRemoved = true;
    }

    /**
     * Informs if this epoch was removed from its consensus instance
     * @return True if it is removed, false otherwise
     */
    public boolean isRemoved() {
        return this.alreadyRemoved;
    }


    public void addToProof(SigShareMessage pm) {
        proof.add(pm);
    }

    public Set<SigShareMessage> getProof() {
        return proof;
    }
    /**
     * Retrieves the duration for the timeout
     * @return Duration for the timeout
     */
    /*public long getTimeout() {
        return this.timeout;
    }*/
    public BigInteger getN() {
        return n;
    }

    public BigInteger getE() {
        return e;
    }
    /**
     * Retrieves this epoch's timestamp
     * @return This epoch's timestamp
     */
    public int getTimestamp() {
        return timestamp;
    }

    /**
     * Retrieves this epoch's consensus
     * @return This epoch's consensus
     */
    public Consensus getConsensus() {
        return consensus;
    }


    /*************************** DEBUG METHODS *******************************/

    public void setPrepareVotesigs(SigShareMessage msg) {
        int p = this.controller.getCurrentViewPos(controller.getCurrentViewPos(msg.getSender()));
        if (p >= 0&&(prepareVotesigs[p]==null|| !prepareVotesigsSetted[p])) {
            prepareVotesigs[p] = msg;
            prepareVotesigsSetted[p] = true;
//            addToProof(msg);
        }
    }
    public void setPreCommitSigs(SigShareMessage msg) {
        int p = this.controller.getCurrentViewPos(controller.getCurrentViewPos(msg.getSender()));
        if (p >= 0&&(preCommitSigs[p]==null|| !preCommitSigsSetted[p])) {
            preCommitSigs[p] = msg;
            preCommitSigsSetted[p] = true;
//            addToProof(msg);
        }
    }
    public void setCommitSigs(SigShareMessage msg) {
        int p = this.controller.getCurrentViewPos(controller.getCurrentViewPos(msg.getSender()));
        if (p >= 0&&(commitSigs[p]==null|| !commitSigsSetted[p])) {
            commitSigs[p] = msg;
            commitSigsSetted[p] = true;
            addToProof(msg);
        }
    }
    public void setThresholdSigPK(BigInteger n, BigInteger e) {
        this.n = n;
        this.e = e;
    }

    public SigShareMessage[] getPrepareVotesigs(int n) {
        SigShareMessage[] sigShareMessages = new SigShareMessage[n];
        int p = 0;
        for (int i = 0; i < prepareVotesigs.length; i++) {
            if (prepareVotesigs[i] != null&&prepareVotesigsSetted[i]) {
                sigShareMessages[p++] = prepareVotesigs[i];
            }
        }
        return sigShareMessages;
    }
    public SigShareMessage[] getPreCommitSigs(int n) {
        SigShareMessage[] sigShareMessages = new SigShareMessage[n];
        int p = 0;
        for (int i = 0; i < preCommitSigs.length; i++) {
            if (preCommitSigs[i] != null&&preCommitSigsSetted[i]) {
                sigShareMessages[p++] = preCommitSigs[i];
            }
        }
        return sigShareMessages;
    }
    public SigShareMessage[] getCommitSigs(int n) {
        SigShareMessage[] sigShareMessages = new SigShareMessage[n];
        int p = 0;
        for (int i = 0; i < commitSigs.length; i++) {
            if (commitSigs[i] != null&&commitSigsSetted[i]) {
                sigShareMessages[p++] = commitSigs[i];
            }
        }
        return sigShareMessages;
    }

    public int countPrepareSignatures(byte[] value, int l, BigInteger n) {
        int count = 0;
        if(n==null){
            return count;
        }
        for (int i = 0; i < prepareVotesigs.length; i++) {

            if (prepareVotesigs[i] != null&& Thresig.checkVerifier(value, prepareVotesigs[i], l, n) ) {//&& ThresholdSignature.checkVerifier(value, sigs[i], l, n)
                prepareVotesigsSetted[i] = true;
                count++;
            } else {
                prepareVotesigsSetted[i] = false;
            }
        }
        return count;
    }

    public int countPreCommitSignatures(byte[] value, int l, BigInteger n) {
        int count = 0;
        if(n==null){
            return count;
        }
        for (int i = 0; i < preCommitSigs.length; i++) {

            if (preCommitSigs[i] != null&& Thresig.checkVerifier(value, preCommitSigs[i], l, n) ) {//&& ThresholdSignature.checkVerifier(value, sigs[i], l, n)
                preCommitSigsSetted[i] = true;
                count++;
            } else {
                preCommitSigsSetted[i] = false;
            }
        }
        return count;
    }

    public int countCommitSignatures(byte[] value, int l, BigInteger n) {
        int count = 0;
        if(n==null){
            return count;
        }
        for (int i = 0; i < commitSigs.length; i++) {

            if (commitSigs[i] != null&& Thresig.checkVerifier(value, commitSigs[i], l, n) ) {//&& ThresholdSignature.checkVerifier(value, sigs[i], l, n)
                commitSigsSetted[i] = true;
                count++;
            } else {
                commitSigsSetted[i] = false;
            }
        }
        return count;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }
    
    /**
     * Clear all epoch info.
     */
    public void clear() {

        int n = controller.getCurrentViewN();

        this.proof = new HashSet<SigShareMessage>();

        this.prepareVotesigs = new SigShareMessage[n];
        this.preCommitSigs = new SigShareMessage[n];
        this.commitSigs = new SigShareMessage[n];
        this.prepareVotesigsSetted = new boolean[n];
        this.preCommitSigsSetted = new boolean[n];
        this.commitSigsSetted = new boolean[n];
        Arrays.fill(prepareVotesigsSetted,false);
        Arrays.fill(preCommitSigsSetted,false);
        Arrays.fill(commitSigsSetted,false);
        this.n = null;
        this.e = null;
    }

    public int getLeader() {
        return leader;
    }

    public void setLeader(int leader) {
        this.leader = leader;
    }
}
