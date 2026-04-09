package com.carsensor.scheduler.application.service.dictionary.impl;

import java.util.Optional;
import org.springframework.stereotype.Service;
import com.carsensor.scheduler.application.service.dictionary.DictionarySearchService;
import com.carsensor.scheduler.domain.dictionary.JapaneseCarDictionary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictionarySearchServiceImpl implements DictionarySearchService {

    private final JapaneseCarDictionary japaneseCarDictionary;

    @Override
    public String findBrandInText(String text) {
        log.debug("Finding brand in text: {}", text);
        Optional<String> brand = japaneseCarDictionary.findBrand(text);
        if (brand.isPresent()) {
            log.debug("Found brand: {} in text", brand.get());
            return brand.get();
        }
        log.debug("No brand found in text");
        return null;
    }

    @Override
    public String findModelInText(String text) {
        log.debug("Finding model in text: {}", text);
        Optional<String> model = japaneseCarDictionary.findModel(text);
        if (model.isPresent()) {
            log.debug("Found model: {} in text", model.get());
            return model.get();
        }
        log.debug("No model found in text");
        return null;
    }
}