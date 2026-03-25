package com.carsensor.scheduler.unit.dictionary;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.carsensor.scheduler.domain.dictionary.JapaneseCarDictionary;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты словаря японских терминов")
class JapaneseCarDictionaryTest {

    private JapaneseCarDictionary dictionary;

    @BeforeEach
    void setUp() {
        dictionary = new JapaneseCarDictionary();
        dictionary.init();
    }

    @Test
    @DisplayName("Инициализация словаря")
    void init_ShouldPopulateAllMappings() {
        assertThat(dictionary.getBrandMapping()).isNotEmpty();
        assertThat(dictionary.getModelMapping()).isNotEmpty();
        assertThat(dictionary.getTransmissionMapping()).isNotEmpty();
        assertThat(dictionary.getDriveTypeMapping()).isNotEmpty();
        assertThat(dictionary.getColorMapping()).isNotEmpty();
    }

    @Test
    @DisplayName("Нормализация марки")
    void normalizeBrand_ShouldConvertJapaneseToEnglish() {
        assertThat(dictionary.normalizeBrand("トヨタ")).isEqualTo("Toyota");
        assertThat(dictionary.normalizeBrand("日産")).isEqualTo("Nissan");
        assertThat(dictionary.normalizeBrand("Unknown")).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("Нормализация модели - точное совпадение")
    void normalizeModel_ExactMatch_ShouldConvert() {
        assertThat(dictionary.normalizeModel("カローラ")).isEqualTo("Corolla");
        assertThat(dictionary.normalizeModel("プリウス")).isEqualTo("Prius");
    }

    @Test
    @DisplayName("Нормализация модели - частичное совпадение")
    void normalizeModel_PartialMatch_ShouldConvert() {
        assertThat(dictionary.normalizeModel("トヨタ カローラ")).isEqualTo("Corolla");
        assertThat(dictionary.normalizeModel("ホンダ フィット")).isEqualTo("Fit");
    }

    @Test
    @DisplayName("Нормализация трансмиссии")
    void normalizeTransmission_ShouldConvert() {
        assertThat(dictionary.normalizeTransmission("AT")).isEqualTo("AT");
        assertThat(dictionary.normalizeTransmission("オートマチック")).isEqualTo("AT");
        assertThat(dictionary.normalizeTransmission("CVT")).isEqualTo("CVT");
        assertThat(dictionary.normalizeTransmission("MT")).isEqualTo("MT");
    }

    @Test
    @DisplayName("Нормализация привода")
    void normalizeDriveType_ShouldConvert() {
        assertThat(dictionary.normalizeDriveType("2WD")).isEqualTo("2WD");
        assertThat(dictionary.normalizeDriveType("FF")).isEqualTo("2WD");
        assertThat(dictionary.normalizeDriveType("4WD")).isEqualTo("4WD");
        assertThat(dictionary.normalizeDriveType("AWD")).isEqualTo("AWD");
    }

    @Test
    @DisplayName("Нормализация цвета")
    void normalizeColor_ShouldConvert() {
        assertThat(dictionary.normalizeColor("ホワイト")).isEqualTo("White");
        assertThat(dictionary.normalizeColor("ブラック")).isEqualTo("Black");
        assertThat(dictionary.normalizeColor("赤")).isEqualTo("Red");
    }

    @Test
    @DisplayName("Поиск марки в тексте")
    void findBrand_ShouldFindBrandInText() {
        assertThat(dictionary.findBrand("トヨタ カローラ")).hasValue("Toyota");
        assertThat(dictionary.findBrand("ホンダ フィット")).hasValue("Honda");
        assertThat(dictionary.findBrand("Unknown")).isEmpty();
    }

    @Test
    @DisplayName("Поиск модели в тексте")
    void findModel_ShouldFindModelInText() {
        assertThat(dictionary.findModel("トヨタ カローラ")).hasValue("Corolla");
        assertThat(dictionary.findModel("ホンダ フィット")).hasValue("Fit");
        assertThat(dictionary.findModel("Unknown")).isEmpty();
    }

    @Test
    @DisplayName("Обновление словаря марок")
    void updateBrandMapping_ShouldReplaceMapping() {
        Map<String, String> newMapping = Map.of("テスト", "Test");
        dictionary.updateBrandMapping(newMapping);

        assertThat(dictionary.getBrandMapping()).hasSize(1);
        assertThat(dictionary.normalizeBrand("テスト")).isEqualTo("Test");
        assertThat(dictionary.normalizeBrand("トヨタ")).isEqualTo("トヨタ");
    }
}