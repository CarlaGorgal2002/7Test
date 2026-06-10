package com.seventest.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataInitializerAccountsTest {

    private static final Set<String> EXPECTED_PAIRS = Set.of(
            pair("tagostinelli", "TomasAgostinelli123"), pair("aaldana", "AngelAldana123"),
            pair("mantolin", "MauricioAntolin123"), pair("rareiza", "RicardoAreiza123"),
            pair("sbacari", "ShimonBacari123"), pair("mbesednjak", "MarioBesednjak123"),
            pair("mbonura", "MatiasBonura123"), pair("ncarreno", "NelsonCarreno123"),
            pair("ncastro", "NicolasCastro123"), pair("vcesareo", "VicenteCesareo123"),
            pair("mconstan", "MariaConstan123"), pair("econtreras", "EsthefanyContreras123"),
            pair("fcravello", "FranciscoCravello123"), pair("bcremona", "BautistaCremona123"),
            pair("ldiaz", "LautaroDiaz123"), pair("bduran", "BrianDuran123"),
            pair("eepstein", "EricEpstein123"), pair("mfina", "MarianoFina123"),
            pair("gfrancisco", "GianlucaFrancisco123"), pair("vgasipi", "ValentinoGasipi123"),
            pair("lgiussani", "LorenzoGiussani123"), pair("cgorgal", "VayanseTodosALaMierda20021995"),
            pair("lgoyer", "LuciaGoyer123"), pair("fgrasso", "FedericoGrasso123"),
            pair("mgueler", "MartinGueler123"), pair("thernandez", "TobiasHernandez123"),
            pair("aiparraguirre", "AlejoIparraguirre123"), pair("plarranaga", "PedroLarranaga123"),
            pair("clombardo", "CarlosLombardo123"), pair("nmartinez", "NicolasMartinez123"),
            pair("fmello", "FacundoMello123"), pair("jperalta", "JeanPaulPeralta123"),
            pair("gperez", "GonzaloPerez123"), pair("lperilli", "LautaroPerilli123"),
            pair("spetrone", "SantinoPetrone123"), pair("cramos", "CarlosRamos123"),
            pair("greartes", "GregorioReartes123"), pair("sreynolds", "StephanieReynolds123"),
            pair("vrodriguez", "VictoriaRodriguez123"), specialPair("mromano", "prof.mromanosc", "MatiasRomano123"),
            pair("mromero", "MartinaRomero123"), pair("esafadie", "EzraSafadie123"),
            pair("vservidio", "ValentinaServidio123"), pair("etaborda", "EmilianoTaborda123"),
            pair("fvechio", "FrancoVechio123")
    );

    @Test
    void containsExactlyTheRequestedStudentTeacherPairs() {
        assertEquals(45, DataInitializer.ACCOUNT_PAIRS.size());

        Set<String> students = DataInitializer.ACCOUNT_PAIRS.stream()
                .map(DataInitializer.AccountPair::studentEmail)
                .collect(Collectors.toSet());
        Set<String> teachers = DataInitializer.ACCOUNT_PAIRS.stream()
                .map(DataInitializer.AccountPair::teacherEmail)
                .collect(Collectors.toSet());

        assertEquals(45, students.size());
        assertEquals(45, teachers.size());
        assertTrue(teachers.contains("prof.mromanosc@uade.edu.ar"));
        assertTrue(students.contains("cgorgal@uade.edu.ar"));
        assertTrue(teachers.contains("prof.cgorgal@uade.edu.ar"));

        Set<String> actualPairs = DataInitializer.ACCOUNT_PAIRS.stream()
                .map(pair -> pair.studentEmail() + "|" + pair.teacherEmail() + "|" + pair.password())
                .collect(Collectors.toSet());
        assertEquals(EXPECTED_PAIRS, actualPairs);
    }

    @Test
    void pabloFariasIsNotPartOfRegularTeacherPairs() {
        assertTrue(DataInitializer.ACCOUNT_PAIRS.stream()
                .noneMatch(pair -> pair.teacherEmail().equalsIgnoreCase("pfarias@uade.edu.ar")));
    }

    private static String pair(String localPart, String password) {
        return specialPair(localPart, "prof." + localPart, password);
    }

    private static String specialPair(String studentLocalPart, String teacherLocalPart, String password) {
        return studentLocalPart + "@uade.edu.ar|" + teacherLocalPart + "@uade.edu.ar|" + password;
    }
}
