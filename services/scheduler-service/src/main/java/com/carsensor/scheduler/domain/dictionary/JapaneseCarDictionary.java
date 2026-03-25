package com.carsensor.scheduler.domain.dictionary;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<String, String> brandMapping = new ConcurrentHashMap<>();
    private final Map<String, String> modelMapping = new ConcurrentHashMap<>();
    private final Map<String, String> transmissionMapping = new ConcurrentHashMap<>();
    private final Map<String, String> driveTypeMapping = new ConcurrentHashMap<>();
    private final Map<String, String> colorMapping = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Инициализация словаря для перевода японских терминов");
        initializeBrands();
        initializeModels();
        initializeTransmissions();
        initializeDriveTypes();
        initializeColors();
    }

    private void initializeBrands() {
        Map.ofEntries(
                Map.entry("トヨタ", "Toyota"),
                Map.entry("日産", "Nissan"),
                Map.entry("ホンダ", "Honda"),
                Map.entry("マツダ", "Mazda"),
                Map.entry("スバル", "Subaru"),
                Map.entry("三菱", "Mitsubishi"),
                Map.entry("スズキ", "Suzuki"),
                Map.entry("ダイハツ", "Daihatsu"),
                Map.entry("いすゞ", "Isuzu"),
                Map.entry("レクサス", "Lexus"),
                Map.entry("BMW", "BMW"),
                Map.entry("メルセデスベンツ", "Mercedes-Benz"),
                Map.entry("アウディ", "Audi"),
                Map.entry("フォルクスワーゲン", "Volkswagen"),
                Map.entry("ポルシェ", "Porsche"),
                Map.entry("ボルボ", "Volvo")
        ).forEach(brandMapping::put);
    }

    private void initializeModels() {
        Map.ofEntries(
                Map.entry("カローラ", "Corolla"),
                Map.entry("プリウス", "Prius"),
                Map.entry("アクア", "Aqua"),
                Map.entry("フィット", "Fit"),
                Map.entry("ヴィッツ", "Vitz"),
                Map.entry("ノート", "Note"),
                Map.entry("セレナ", "Serena"),
                Map.entry("エスティマ", "Estima"),
                Map.entry("アルファード", "Alphard"),
                Map.entry("ヴェルファイア", "Vellfire"),
                Map.entry("ランドクルーザー", "Land Cruiser"),
                Map.entry("ハイエース", "Hiace"),
                Map.entry("アクセラ", "Axela"),
                Map.entry("デミオ", "Demio"),
                Map.entry("CX-5", "CX-5"),
                Map.entry("インプレッサ", "Impreza"),
                Map.entry("レガシィ", "Legacy"),
                Map.entry("フォレスター", "Forester")
        ).forEach(modelMapping::put);
    }

    private void initializeTransmissions() {
        Map.ofEntries(
                Map.entry("AT", "AT"),
                Map.entry("オートマチック", "AT"),
                Map.entry("CVT", "CVT"),
                Map.entry("無段変速機", "CVT"),
                Map.entry("MT", "MT"),
                Map.entry("マニュアル", "MT"),
                Map.entry("DCT", "DCT"),
                Map.entry("デュアルクラッチ", "DCT")
        ).forEach(transmissionMapping::put);
    }

    private void initializeDriveTypes() {
        Map.ofEntries(
                Map.entry("2WD", "2WD"),
                Map.entry("FF", "2WD"),
                Map.entry("フロント", "2WD"),
                Map.entry("4WD", "4WD"),
                Map.entry("四輪駆動", "4WD"),
                Map.entry("AWD", "AWD"),
                Map.entry("全輪駆動", "AWD")
        ).forEach(driveTypeMapping::put);
    }

    private void initializeColors() {
        Map.ofEntries(
                Map.entry("ホワイト", "White"),
                Map.entry("白", "White"),
                Map.entry("ブラック", "Black"),
                Map.entry("黒", "Black"),
                Map.entry("シルバー", "Silver"),
                Map.entry("銀", "Silver"),
                Map.entry("グレー", "Gray"),
                Map.entry("ガンメタ", "Gray"),
                Map.entry("レッド", "Red"),
                Map.entry("赤", "Red"),
                Map.entry("ブルー", "Blue"),
                Map.entry("青", "Blue"),
                Map.entry("グリーン", "Green"),
                Map.entry("緑", "Green"),
                Map.entry("イエロー", "Yellow"),
                Map.entry("黄", "Yellow"),
                Map.entry("ベージュ", "Beige"),
                Map.entry("ゴールド", "Gold"),
                Map.entry("パール", "Pearl"),
                Map.entry("メタリック", "Metallic")
        ).forEach(colorMapping::put);
    }

    public String normalizeBrand(String japaneseBrand) {
        return brandMapping.getOrDefault(japaneseBrand, japaneseBrand);
    }

    public String normalizeModel(String japaneseModel) {
        return Optional.ofNullable(modelMapping.get(japaneseModel))
                .orElseGet(() -> findPartialMatch(japaneseModel));
    }

    private String findPartialMatch(String japaneseModel) {
        return modelMapping.entrySet().stream()
                .filter(entry -> japaneseModel.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(japaneseModel);
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
        return brandMapping.entrySet().stream()
                .filter(entry -> text.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public Optional<String> findModel(String text) {
        return modelMapping.entrySet().stream()
                .filter(entry -> text.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    // Методы для динамического обновления словаря
    public void updateBrandMapping(Map<String, String> newMapping) {
        updateMapping(this.brandMapping, newMapping);
    }

    public void updateModelMapping(Map<String, String> newMapping) {
        updateMapping(this.modelMapping, newMapping);
    }

    public void updateTransmissionMapping(Map<String, String> newMapping) {
        updateMapping(this.transmissionMapping, newMapping);
    }

    public void updateDriveTypeMapping(Map<String, String> newMapping) {
        updateMapping(this.driveTypeMapping, newMapping);
    }

    public void updateColorMapping(Map<String, String> newMapping) {
        updateMapping(this.colorMapping, newMapping);
    }

    private void updateMapping(Map<String, String> target, Map<String, String> source) {
        target.clear();
        target.putAll(source);
    }
}