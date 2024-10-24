package cn.xbhel;

import cn.xbhel.model.PracticeArea;
import cn.xbhel.model.PracticeAreas;
import cn.xbhel.util.FileUtils;
import jakarta.xml.bind.JAXB;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class Application {

    public static void main(String[] args) throws IOException {
        PracticeAreas unmarshal = JAXB.unmarshal(
                FileUtils.newBufferStream("classpath:topic-tree-mapping.xml"), PracticeAreas.class);
        System.out.println(unmarshal);
    }

}
