package com.carsensor.scheduler.domain.dictionary;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Словарь для перевода японских терминов в англоязычные/нормализованные значения
 */
@Slf4j
@Getter
@Component
public class JapaneseCarDictionary {

    @Getter
    private final Map<String, String> brandMapping = new HashMap<>();

    @Getter
    private final Map<String, String> modelMapping = new HashMap<>();

    @Getter
    private final Map<String, String> transmissionMapping = new HashMap<>();

    @Getter
    private final Map<String, String> driveTypeMapping = new HashMap<>();

    @Getter
    private final Map<String, String> colorMapping = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("Инициализация словаря для перевода японских терминов");

        // Марки автомобилей
        brandMapping.put("トヨタ", "Toyota");
        brandMapping.put("日産", "Nissan");
        brandMapping.put("ホンダ", "Honda");
        brandMapping.put("マツダ", "Mazda");
        brandMapping.put("スバル", "Subaru");
        brandMapping.put("三菱", "Mitsubishi");
        brandMapping.put("スズキ", "Suzuki");
        brandMapping.put("ダイハツ", "Daihatsu");
        brandMapping.put("いすゞ", "Isuzu");
        brandMapping.put("レクサス", "Lexus");
        brandMapping.put("BMW", "BMW");
        brandMapping.put("メルセデスベンツ", "Mercedes-Benz");
        brandMapping.put("アウディ", "Audi");
        brandMapping.put("フォルクスワーゲン", "Volkswagen");
        brandMapping.put("ポルシェ", "Porsche");
        brandMapping.put("ボルボ", "Volvo");

        // Популярные модели
        modelMapping.put("カローラ", "Corolla");
        modelMapping.put("プリウス", "Prius");
        modelMapping.put("アクア", "Aqua");
        modelMapping.put("フィット", "Fit");
        modelMapping.put("ヴィッツ", "Vitz");
        modelMapping.put("ノート", "Note");
        modelMapping.put("セレナ", "Serena");
        modelMapping.put("エスティマ", "Estima");
        modelMapping.put("アルファード", "Alphard");
        modelMapping.put("ヴェルファイア", "Vellfire");
        modelMapping.put("ランドクルーザー", "Land Cruiser");
        modelMapping.put("ハイエース", "Hiace");
        modelMapping.put("アクセラ", "Axela");
        modelMapping.put("デミオ", "Demio");
        modelMapping.put("CX-5", "CX-5");
        modelMapping.put("インプレッサ", "Impreza");
        modelMapping.put("レガシィ", "Legacy");
        modelMapping.put("フォレスター", "Forester");

        // Типы трансмиссии
        transmissionMapping.put("AT", "AT");
        transmissionMapping.put("オートマチック", "AT");
        transmissionMapping.put("CVT", "CVT");
        transmissionMapping.put("無段変速機", "CVT");
        transmissionMapping.put("MT", "MT");
        transmissionMapping.put("マニュアル", "MT");
        transmissionMapping.put("DCT", "DCT");
        transmissionMapping.put("デュアルクラッチ", "DCT");

        // Типы привода
        driveTypeMapping.put("2WD", "2WD");
        driveTypeMapping.put("FF", "2WD");
        driveTypeMapping.put("フロント", "2WD");
        driveTypeMapping.put("4WD", "4WD");
        driveTypeMapping.put("四輪駆動", "4WD");
        driveTypeMapping.put("AWD", "AWD");
        driveTypeMapping.put("全輪駆動", "AWD");

        // Цвета
        colorMapping.put("ホワイト", "White");
        colorMapping.put("白", "White");
        colorMapping.put("ブラック", "Black");
        colorMapping.put("黒", "Black");
        colorMapping.put("シルバー", "Silver");
        colorMapping.put("銀", "Silver");
        colorMapping.put("グレー", "Gray");
        colorMapping.put("ガンメタ", "Gray");
        colorMapping.put("レッド", "Red");
        colorMapping.put("赤", "Red");
        colorMapping.put("ブルー", "Blue");
        colorMapping.put("青", "Blue");
        colorMapping.put("グリーン", "Green");
        colorMapping.put("緑", "Green");
        colorMapping.put("イエロー", "Yellow");
        colorMapping.put("黄", "Yellow");
        colorMapping.put("ベージュ", "Beige");
        colorMapping.put("ゴールド", "Gold");
        colorMapping.put("パール", "Pearl");
        colorMapping.put("メタリック", "Metallic");
    }

    public String normalizeBrand(String japaneseBrand) {
        return brandMapping.getOrDefault(japaneseBrand, japaneseBrand);
    }

    public String normalizeModel(String japaneseModel) {
        // Сначала проверяем точное соответствие
        String normalized = modelMapping.get(japaneseModel);
        if (normalized != null) {
            return normalized;
        }

        // Пытаемся найти частичное совпадение
        for (Map.Entry<String, String> entry : modelMapping.entrySet()) {
            if (japaneseModel.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return japaneseModel;
    }

    public String normalizeTransmission(String japaneseTransmission) {
        return transmissionMapping.getOrDefault(japaneseTransmission, japaneseTransmission);
    }

    public String normalizeDriveType(String japaneseDriveType) {
        return driveTypeMapping.getOrDefault(japaneseDriveType, japaneseDriveType);
    }

    public String normalizeColor(String japaneseColor) {
        return colorMapping.getOrDefault(japaneseColor, japaneseColor);
    }

    public Optional<String> findBrand(String text) {
        return brandMapping.keySet().stream()
                .filter(key -> text.contains(key))
                .findFirst()
                .map(brandMapping::get);
    }

    public Optional<String> findModel(String text) {
        return modelMapping.keySet().stream()
                .filter(key -> text.contains(key))
                .findFirst()
                .map(modelMapping::get);
    }

    // Методы для динамического обновления словаря
    public void updateBrandMapping(Map<String, String> newMapping) {
        this.brandMapping.clear();
        this.brandMapping.putAll(newMapping);
    }

    public void updateModelMapping(Map<String, String> newMapping) {
        this.modelMapping.clear();
        this.modelMapping.putAll(newMapping);
    }

    public void updateTransmissionMapping(Map<String, String> newMapping) {
        this.transmissionMapping.clear();
        this.transmissionMapping.putAll(newMapping);
    }

    public void updateDriveTypeMapping(Map<String, String> newMapping) {
        this.driveTypeMapping.clear();
        this.driveTypeMapping.putAll(newMapping);
    }

    public void updateColorMapping(Map<String, String> newMapping) {
        this.colorMapping.clear();
        this.colorMapping.putAll(newMapping);
    }
}