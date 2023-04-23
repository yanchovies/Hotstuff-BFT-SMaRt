package hotstuff.consensus.messages;

import hotstuff.communication.SystemMessage;
import hotstuff.tom.util.TOMUtil;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigInteger;
import java.util.Objects;

/**
 * @Author Moonk
 * @Date 2022/6/19
 */
public class SigShareMessage extends ConsensusMessage {
    private static final long serialVersionUID = -5470264760450037954L;
    private int id;
    private BigInteger sig;
    private BigInteger z;
    private BigInteger c;
    private BigInteger verifier;
    private BigInteger groupVerifier;
    public SigShareMessage() {
    }
    public SigShareMessage(int from, final int cid,final int epoch, final int type,final int id, final BigInteger sig, BigInteger z, BigInteger c, BigInteger verifier, BigInteger groupVerifier) {
        super(type,cid,epoch, from);
        this.id = id;
        this.sig = sig;
        this.z = z;
        this.c = c;
        this.verifier = verifier;
        this.groupVerifier = groupVerifier;
    }
    public BigInteger getZ() {
        return z;
    }

    public BigInteger getC() {
        return c;
    }

    public BigInteger getVerifier() {
        return verifier;
    }

    public BigInteger getGroupVerifier() {
        return groupVerifier;
    }

    public int getId() {
        return this.id;
    }
    public BigInteger getSig() {
        return this.sig;
    }
    @Override
    public String toString() {
        return "SigShareMessage{" +
                "from=" + sender +
                ", number =" + super.getNumber() +
                ", type =" + super.getType() +
                '}';
    }
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(id);
        byte[] bytes = TOMUtil.toByteArray(sig);
        out.writeInt(bytes.length);
        out.write(bytes);
        bytes = TOMUtil.toByteArray(z);
        out.writeInt(bytes.length);
        out.write(bytes);
        bytes = TOMUtil.toByteArray(c);
        out.writeInt(bytes.length);
        out.write(bytes);
        bytes = TOMUtil.toByteArray(verifier);
        out.writeInt(bytes.length);
        out.write(bytes);
        bytes = TOMUtil.toByteArray(groupVerifier);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        id = in.readInt();
        int toRead = in.readInt();
        if(toRead != -1) {
            byte[] value= new byte[toRead];
            do{
                toRead -= in.read(value, value.length-toRead, toRead);

            } while(toRead > 0);
            sig = new BigInteger(1,value);
        }

        toRead = in.readInt();
        if (toRead != -1) {
            byte[] value = new byte[toRead];
            do {
                toRead -= in.read(value, value.length - toRead, toRead);

            } while (toRead > 0);
            z = new BigInteger(1, value);
        }

        toRead = in.readInt();
        if (toRead != -1) {
            byte[] value = new byte[toRead];
            do {
                toRead -= in.read(value, value.length - toRead, toRead);

            } while (toRead > 0);
            c = new BigInteger(1, value);
        }
        toRead = in.readInt();
        if (toRead != -1) {
            byte[] value = new byte[toRead];
            do {
                toRead -= in.read(value, value.length - toRead, toRead);

            } while (toRead > 0);
            verifier = new BigInteger(1, value);
        }
        toRead = in.readInt();
        if (toRead != -1) {
            byte[] value = new byte[toRead];
            do {
                toRead -= in.read(value, value.length - toRead, toRead);
            } while (toRead > 0);
            groupVerifier = new BigInteger(1, value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SigShareMessage that = (SigShareMessage) o;
        return id == that.id &&
                Objects.equals(sig, that.sig) &&
                Objects.equals(z, that.z) &&
                Objects.equals(c, that.c) &&
                Objects.equals(verifier, that.verifier) &&
                Objects.equals(groupVerifier, that.groupVerifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash( id, sig, z, c, verifier, groupVerifier);
    }
}
