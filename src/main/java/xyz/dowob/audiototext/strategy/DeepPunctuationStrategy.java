package xyz.dowob.audiototext.strategy;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * @author yuan
 * @program AudioToText
 * @ClassName DeepPunctuationStrategy
 * @description
 * @create 2024-12-18 12:27
 * @Version 1.0
 **/
//todo 自動下載模型
@Log4j2
@Component
public class DeepPunctuationStrategy implements PunctuationStrategy {

    public DeepPunctuationStrategy() {

    }


    @Override
    public String addPunctuation(String text) throws Exception {
        return null;
    }

    @Override
    public String getModelName() {
        return null;
    }


}
