package cn.xbhel.xml;

import org.w3c.dom.Node;

public interface NodeHandler<A> {
    Object handle(Node source, A attachment);

}
