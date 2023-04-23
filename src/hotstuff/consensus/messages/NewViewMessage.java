package hotstuff.consensus.messages;

import hotstuff.communication.SystemMessage;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @Author Moonk
 * @Date 2022/11/17
 */
public class NewViewMessage extends ConsensusMessage {
    private static final long serialVersionUID = -1884788878010023959L;
    private int newLeaderId;

    public NewViewMessage() {
    }

    public NewViewMessage(int from, int type, int newLeaderId, int number, int epoch) {
        super(type,number,epoch,from);
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
                ", number =" + super.getNumber() +
                ", type =" + super.getType() +
                '}';
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(sender);
        out.writeInt(newLeaderId);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        sender = in.readInt();
        newLeaderId = in.readInt();
    }

}
