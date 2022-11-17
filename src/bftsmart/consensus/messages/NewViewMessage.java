package bftsmart.consensus.messages;

import bftsmart.communication.SystemMessage;
import bftsmart.tom.util.TOMUtil;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigInteger;

/**
 * @Author Moonk
 * @Date 2022/11/17
 */
public class NewViewMessage extends SystemMessage {
    private static final long serialVersionUID = -1884788878010023959L;
    private int newLeaderId;

    public NewViewMessage() {
    }

    public NewViewMessage(int newLeaderId) {
        this.newLeaderId = newLeaderId;
    }

    public int getNewLeaderId() {
        return newLeaderId;
    }

    @Override
    public String toString() {
        return "NewViewMessage{" +
                "newLeaderId=" + newLeaderId +
                ", sender=" + sender +
                '}';
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(sender);
        out.writeInt(newLeaderId);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        sender = in.readInt();
        newLeaderId = in.readInt();
    }

}
