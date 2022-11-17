package bftsmart.consensus.messages;

import bftsmart.communication.SystemMessage;
import bftsmart.tom.util.TOMUtil;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigInteger;

/**
 * @Author Moonk
 * @Date 2022/6/19
 */
public class KeyShareMessage extends SystemMessage {
    private static final long serialVersionUID = 658968461577841206L;

    private BigInteger secret;
    private int id;
    private BigInteger verifier;
    private BigInteger groupVerifier;
    private BigInteger e;
    private BigInteger n;
    private int l;
    private boolean isLeaderMessage = false;

    public KeyShareMessage(final int id, final BigInteger secret) {
        this.id = id;
        this.secret = secret;
    }


    public int getL() {
        return l;
    }

    public boolean isLeaderMessage() {
        return isLeaderMessage;
    }

    public KeyShareMessage() {
    }

    public void setVerifiers(final BigInteger verifier,
                             final BigInteger groupVerifier) {
        this.verifier = verifier;
        this.groupVerifier = groupVerifier;
    }
    public void setParameter(final BigInteger e, final BigInteger n,int l,boolean isLeaderMessage) {
        this.e = e;
        this.n = n;
        this.l = l;
        this.isLeaderMessage = isLeaderMessage;
    }

    public BigInteger getE() {
        return e;
    }

    public BigInteger getN() {
        return n;
    }

    public BigInteger getGroupVerifier() {
        return groupVerifier;
    }

    public BigInteger getVerifier() {
        return verifier;
    }

    public int getId() {
        return id;
    }

    public BigInteger getSecret() {
        return secret;
    }

    @Override
    public String toString() {
        return "KeyShareMessage{" +
                "from=" + sender +
                '}';
    }
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        out.writeInt(sender);
        out.writeInt(id);
        out.writeBoolean(isLeaderMessage);
        out.writeInt(l);
        byte[] bytes = TOMUtil.toByteArray(secret);
        out.writeInt(bytes.length);
        out.write(bytes);
        bytes = TOMUtil.toByteArray(verifier);
        out.writeInt(bytes.length);
        out.write(bytes);
        bytes = TOMUtil.toByteArray(groupVerifier);
        out.writeInt(bytes.length);
        out.write(bytes);
        bytes = TOMUtil.toByteArray(e);
        out.writeInt(bytes.length);
        out.write(bytes);
        bytes = TOMUtil.toByteArray(n);
        out.writeInt(bytes.length);
        out.write(bytes);

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        sender=in.readInt();
        id = in.readInt();
        isLeaderMessage = in.readBoolean();
        l = in.readInt();
        int toRead = in.readInt();
        if(toRead != -1) {
            byte[] value= new byte[toRead];
            do{
                toRead -= in.read(value, value.length-toRead, toRead);

            } while(toRead > 0);
            secret = new BigInteger(1,value);
        }
        toRead = in.readInt();
        if(toRead != -1) {
            byte[] value= new byte[toRead];
            do{
                toRead -= in.read(value, value.length-toRead, toRead);

            } while(toRead > 0);
            verifier = new BigInteger(1,value);
        }
        toRead = in.readInt();
        if(toRead != -1) {
            byte[] value= new byte[toRead];
            do{
                toRead -= in.read(value, value.length-toRead, toRead);
            } while(toRead > 0);
            groupVerifier = new BigInteger(1,value);
        }
        toRead = in.readInt();
        if(toRead != -1) {
            byte[] value= new byte[toRead];
            do{
                toRead -= in.read(value, value.length-toRead, toRead);
            } while(toRead > 0);
            e = new BigInteger(1,value);
        }
        toRead = in.readInt();
        if(toRead != -1) {
            byte[] value= new byte[toRead];
            do{
                toRead -= in.read(value, value.length-toRead, toRead);
            } while(toRead > 0);
            n = new BigInteger(1,value);
        }




//        secret = new BigInteger(1,in.readByte());

//        verifier = (BigInteger) in.readObject();
//        groupVerifier = (BigInteger) in.readObject();
//        e = (BigInteger) in.readObject();
//        n = (BigInteger) in.readObject();

    }
}
