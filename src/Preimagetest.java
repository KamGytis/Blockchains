import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 7. Spėjimas (preimage).
 *
 * a) Be druskos: kandidatai "0000".."9999", lyginami su tiksline maisa.
 *    Uzfiksuojama: bandymu skaicius, laikas, visi sutampantys kandidatai
 *    (naudojant sutrumpinta maisa, kad butu realu rasti daugiau nei viena
 *    sutampanti reiksme ir parodyti, kodel tai svarbu).
 * b) Pakartota su viesa druska (salt), pridedama prie kiekvieno kandidato
 *    pries maisant. Aptariama, kodel vieša druska neapsunkina atakos prieš
 *    viena konkretu taikini (uzpuolikas tiesiog pridedą ta pacia zinoma
 *    druska prie kiekvieno kandidato - kaina ta pati), bet apsunkina
 *    is anksto suskaiciuotu lenteliu (rainbow table) pakartotini panaudojima
 *    pries kelis taikinius, jei kiekvienam taikiniui druska skirtinga.
 * c) Aptariamas slaptas r (pepper) - skirtumas nuo druskos: jis nebuna
 *    saugomas kartu su maisa ir ne zinomas uzpuolikui, todel realiai
 *    padidina paieskos erdve (uzpuolikas turi atspeti ir kandidata, ir r).
 */
public class Preimagetest {

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        // a) be druskos - pilna maisa, vienas taikinys

        System.out.println(" a) Preimage paieska be druskos (pilna 256 b. maisa) ");
        String secretPin = "4821";
        String target = Hash.hash(secretPin.getBytes(StandardCharsets.UTF_8));
        BruteForceResult r1 = bruteForcePin(target, null);
        System.out.println("Taikinys: " + target);
        System.out.println("Bandymu: " + r1.attempts + ", laikas: " + r1.millis + " ms");
        System.out.println("Sutampantys kandidatai (pilna maisa): " + r1.matches);

        //  sutrumpinta maisa, kad parodytume daugiau nei viena atitikme
        System.out.println("\nTa pati paieska, bet lyginant tik pirmus 12 bitu (3 hex simbolius)");
        System.out.println("   (parodo, kodel reikia fiksuoti VISUS sutampancius kandidatus, ne tik pirma)");
        String targetShort = target.substring(0, 3);
        BruteForceResult r1short = bruteForcePinTruncated(targetShort, null, 3);
        System.out.println("Bandymu: " + r1short.attempts + ", laikas: " + r1short.millis + " ms");
        System.out.println("Sutampantys kandidatai (is 10000): " + r1short.matches
                + " (is kuriu tikrasis pin yra tik vienas - likusieji yra atsitiktines sutapimai)");

        // b) su viesa druska - vienas taikinys

        System.out.println("\n b) Ta pati paieska SU viesa druska (viena, fiksuota sitam taikiniui) ");
        String salt = "b7f3-vieša-druska-01";
        String target2 = Hash.hash((salt + secretPin).getBytes(StandardCharsets.UTF_8));
        BruteForceResult r2 = bruteForcePin(target2, salt);
        System.out.println("Druska (vieša, zinoma uzpuolikui): \"" + salt + "\"");
        System.out.println("Taikinys: " + target2);
        System.out.println("Bandymu: " + r2.attempts + ", laikas: " + r2.millis + " ms");
        System.out.println("Sutampantys kandidatai: " + r2.matches);
        System.out.println("Isvada: bandymu skaicius ir laikas prakitskai toks pat kaip be druskos (" + r1.attempts
                + " vs " + r2.attempts + ") - viesa druska neapsunkina atakos prieš sita viena taikini, nes "
                + "uzpuolikas tiesiog prideda zinoma druska prie kiekvieno kandidato ta pacia kaina.");

        // c)viesos drusks nauda: keli taikiniai su skirtinga druska

        System.out.println("\n c) Keli taikiniai - kodel druska turi buti skirtinga kiekvienam ");
        int targetsCount = 5;
        String[] pins = { "4821", "0193", "7765", "2222", "9001" };
        long startShared = System.nanoTime();
        // scenarijus 1: be druskos - uzpuolikas viena karta suskaiciuoja hash(kandidatas)
        // visiems 10000 kandidatu ir gauna "lentele", kuria panaudoja VISIEMS taikiniams is karto
        List<String> allHashesNoSalt = new ArrayList<>(10_000);
        for (int i = 0; i < 10_000; i++) {
            allHashesNoSalt.add(Hash.hash(String.format("%04d", i).getBytes(StandardCharsets.UTF_8)));
        }
        long sharedTableMs = (System.nanoTime() - startShared) / 1_000_000;
        int foundNoSalt = 0;
        for (String pin : pins) {
            String t = Hash.hash(pin.getBytes(StandardCharsets.UTF_8));
            if (allHashesNoSalt.contains(t)) foundNoSalt++;
        }
        System.out.println("be druskos: viena 10000 irasu lentele (suskaiciuota per " + sharedTableMs
                + " ms) iskart 'atrakina' visus " + foundNoSalt + "/" + targetsCount + " taikinius - "
                + "lentele apskaiciuojama viena karta ir panaudojama pakartotinai.");

        // scenarijus 2: kiekvienas taikinys turi savo atskira, atsitiktine druska
        Random rnd = new Random(99);
        long totalAttemptsPerTarget = 0;
        long totalMsWithSalt = 0;
        for (String pin : pins) {
            String perTargetSalt = "salt-" + rnd.nextInt(1_000_000);
            String t = Hash.hash((perTargetSalt + pin).getBytes(StandardCharsets.UTF_8));
            BruteForceResult r = bruteForcePin(t, perTargetSalt);
            totalAttemptsPerTarget += r.attempts;
            totalMsWithSalt += r.millis;
        }
        System.out.println("su skirtinga druska kiekvienam taikiniui: uzpuolikas privalo pakartoti visa 10000 "
                + "bandymu paieska kiekvienam taikiniui atskirai (is viso " + totalAttemptsPerTarget
                + " bandymu " + targetsCount + " taikiniams, " + totalMsWithSalt + " ms), nes anksciau "
                + "suskaiciuota lentele su kita druska yra nenaudinga naujam taikiniui.");

        // d) slaptas r (pepper) - skirtumas nuo druskos
        System.out.println("\n d) Slaptas r (pepper) ");
        String secretPepper = "r-" + new Random(7).nextInt(1_000_000); // zinomas tik 'serveriui'
        String target3 = Hash.hash((secretPepper + secretPin).getBytes(StandardCharsets.UTF_8));
        System.out.println("Taikinys (su paslėptu r, kurio uzpuolikas nezino): " + target3);
        System.out.println("Bandome PIN brute-force kaip anksciau, bet be druskos/r zinojimo (kaip uzpuolikas):");
        BruteForceResult r3 = bruteForcePin(target3, null); // uzpuolikas neprideda r, nes jo nezino
        System.out.println("Bandymu: " + r3.attempts + ", rasta sutampanciu kandidatu: " + r3.matches
                + " (turetu buti 0 - be teisingo r uzpuolikas apskritai negali apskaiciuoti teisingo kandidato hash)");
    }

    static class BruteForceResult {
        long attempts;
        long millis;
        List<String> matches = new ArrayList<>();
    }

    static BruteForceResult bruteForcePin(String target, String salt) {
        BruteForceResult result = new BruteForceResult();
        long start = System.nanoTime();
        long attempts = 0;
        for (int i = 0; i <= 9999; i++) {
            String candidate = String.format("%04d", i);
            String toHash = (salt == null ? candidate : salt + candidate);
            attempts++;
            String h = Hash.hash(toHash.getBytes(StandardCharsets.UTF_8));
            if (h.equals(target)) {
                result.matches.add(candidate);
            }
        }
        result.attempts = attempts;
        result.millis = (System.nanoTime() - start) / 1_000_000;
        return result;
    }

    static BruteForceResult bruteForcePinTruncated(String targetPrefix, String salt, int hexChars) {
        BruteForceResult result = new BruteForceResult();
        long start = System.nanoTime();
        long attempts = 0;
        for (int i = 0; i <= 9999; i++) {
            String candidate = String.format("%04d", i);
            String toHash = (salt == null ? candidate : salt + candidate);
            attempts++;
            String h = Hash.hash(toHash.getBytes(StandardCharsets.UTF_8));
            if (h.substring(0, hexChars).equals(targetPrefix)) {
                result.matches.add(candidate);
            }
        }
        result.attempts = attempts;
        result.millis = (System.nanoTime() - start) / 1_000_000;
        return result;
    }
}