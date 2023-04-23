package hotstuff.demo.EVsharing;

import java.io.Serializable;

/**
 * This helper class implements a quartet of objects.
 *
 */

public class Quartet<A, B, C, D> implements Serializable {

    private static final long serialVersionUID = 3L;
    private A first;
    private B second;
    private C third;
    private D fourth;

    public Quartet(A first, B second, C third, D fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    public C getThird() {
        return third;
    }

    public D getFourth() {
        return fourth;
    }

}
