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
package hotstuff.tom.leaderchange;

import hotstuff.consensus.messages.ConsensusMessage;
import hotstuff.consensus.messages.SigShareMessage;
import hotstuff.tom.util.TOMUtil;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigInteger;
import java.util.Set;

/**
 * Data about the last consensus decision
 *
 * @author Joao Sousa
 */
public class CertifiedDecision implements Externalizable {

    private int pid; // process id
    private int cid; // execution id
    private byte[] decision; // decision value
    private Set<SigShareMessage> sigsMsgs; // proof of the decision
    private BigInteger n;//threshold signature public key
    private BigInteger e;//threshold signature public key

    /**
     * Empty constructor
     */
    public CertifiedDecision() {
        pid = -1;
        cid = -1;
        decision = null;
        sigsMsgs = null;
        n = new BigInteger("-1");
        e = new BigInteger("-1");
    }

    /**
     * Constructor
     *
     * @param pid process id
     * @param cid execution id
     * @param decision decision value
     * @param sigsMsgs proof of the decision in the form of authenticated Consensus Messages
     */
    public CertifiedDecision(int pid, int cid, byte[] decision, Set<SigShareMessage> sigsMsgs, BigInteger n, BigInteger e) {

        this.pid = pid;
        this.cid = cid;
        this.decision = decision;
        this.sigsMsgs = sigsMsgs;
        this.e = e;
        this.n = n;
    }

    /**
     * Get consensus ID
     * @return consensus ID
     */
    public int getCID() {
        return cid;
    }

    /**
     * Get decision value
     * @return decision value
     */
    public byte[] getDecision() {
        return decision;
    }

    /**
     * Get proof of the decision in the form of authenticated Consensus Messages
     * @return proof of the decision in the form of authenticated Consensus Messages
     */
    public Set<SigShareMessage> getSigsMsgs() {
        return sigsMsgs;
    }

    public BigInteger getN() {
        return n;
    }

    public BigInteger getE() {
        return e;
    }
    /**
     * Get process id
     * @return process id
     */
    public int getPID() {
        return pid;
    }
    public boolean equals(Object obj) {

        if (obj instanceof CertifiedDecision) {

            CertifiedDecision cDec = (CertifiedDecision) obj;

            if (cDec.pid == pid) return true;
        }

        return false;
    }

    public int hashCode() {
        return pid;
    }

    public void writeExternal(ObjectOutput out) throws IOException {

        out.writeInt(pid);
        out.writeInt(cid);
        out.writeObject(decision);
        out.writeObject(sigsMsgs);
        if (e != null) {
            byte[] bytes = TOMUtil.toByteArray(e);
            out.writeInt(bytes.length);
            out.write(bytes);
        } else {
            out.writeInt(-1);
        }
        if (n != null) {
            byte[] bytes = TOMUtil.toByteArray(n);
            out.writeInt(bytes.length);
            out.write(bytes);
        } else {
            out.writeInt(-1);
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        pid = in.readInt();
        cid = in.readInt();
        decision = (byte[]) in.readObject();
        sigsMsgs = (Set<SigShareMessage>) in.readObject();
        int toRead = in.readInt();
        if (toRead != -1) {
            byte[] value = new byte[toRead];
            do {
                toRead -= in.read(value, value.length - toRead, toRead);
            } while (toRead > 0);
            e = new BigInteger(1, value);
        }
        toRead = in.readInt();
        if (toRead != -1) {
            byte[] value = new byte[toRead];
            do {
                toRead -= in.read(value, value.length - toRead, toRead);
            } while (toRead > 0);
            n = new BigInteger(1, value);
        }
    }
}
