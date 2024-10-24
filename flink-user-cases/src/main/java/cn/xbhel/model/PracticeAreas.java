package cn.xbhel.model;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.List;

@Data
@XmlRootElement(name = "practiceareas")
@XmlAccessorType(XmlAccessType.FIELD)
public class PracticeAreas {

    @XmlElement(name = "practicearea")
    private List<PracticeArea> practiceAreaList;

}
