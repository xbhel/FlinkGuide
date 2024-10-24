package cn.xbhel.xml;


import org.dom4j.Node;

@FunctionalInterface
public interface NodeSelector<T extends Node> {

    Node select(T source);

}
