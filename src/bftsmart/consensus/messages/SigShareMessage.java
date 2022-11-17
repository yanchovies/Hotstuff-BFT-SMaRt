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
public class SigShareMessage extends SystemMessage {
    private static final long serialVersionUID = -5470264760450037954L;
    private int number;
    private int id;
    private BigInteger sig;
    private int epoch;
    private int type;
    public SigShareMessage() {
    }
    public SigShareMessage(final int cid,final int epoch, final int type,final int id, final BigInteger sig) {
        this.number = cid;
        this.epoch = epoch;
        this.id = id;
        this.type = type;
        this.sig = sig;
    }

    public int getEpoch() {
        return epoch;
    }

    public int getNumber() {
        return number;
    }

    public int getType() {
        return type;
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
                ",number=" + number +
                '}';
    }
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(sender);
        out.writeInt(number);
        out.writeInt(id);
        out.writeInt(epoch);
        out.writeInt(type);
        byte[] bytes = TOMUtil.toByteArray(sig);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        sender = in.readInt();
        number = in.readInt();
        id = in.readInt();
        epoch = in.readInt();
        type = in.readInt();
        int toRead = in.readInt();
        if(toRead != -1) {
            byte[] value= new byte[toRead];
            do{
                toRead -= in.read(value, value.length-toRead, toRead);

            } while(toRead > 0);
            sig = new BigInteger(1,value);
        }
    }

}
