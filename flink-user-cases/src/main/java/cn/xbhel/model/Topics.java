package cn.xbhel.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Topics {

    @XmlElement(name = "topic")
    private List<Topic> topicList;

    @XmlElement(name = "topics")
    private List<Topics> topicsList;

}
