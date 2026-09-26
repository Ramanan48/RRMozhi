package com.ramanan.mozhi

/**
 * Common spoken-Tamil words as people actually type them in Tanglish, with their spelling variants.
 * These are converted directly (always correct), so only unusual words go to the online/offline converter.
 * Keys are lowercase. Add new words here: "spelling1|spelling2" to "தமிழ்".
 */
object TanglishDictionary {

    private val words: List<Pair<String, String>> = listOf(
        // ---- people / pronouns
        "naan|nan|na" to "நான்", "nee|ni" to "நீ", "neenga|nenga|neengal|ninga" to "நீங்க",
        "naanga|nanga" to "நாங்க", "naama|nama" to "நாம", "namma" to "நம்ம",
        "avan" to "அவன்", "ava" to "அவ", "aval" to "அவள்", "avanga|avunga" to "அவங்க", "avar" to "அவர்",
        "enaku|enakku|yenaku|yenakku" to "எனக்கு", "enakum|enakkum" to "எனக்கும்",
        "unaku|unakku" to "உனக்கு", "ungaluku|ungalukku" to "உங்களுக்கு",
        "avanuku|avanukku" to "அவனுக்கு", "avaluku|avalukku" to "அவளுக்கு",
        "en" to "என்", "un" to "உன்", "unga" to "உங்க",
        "ennoda|ennuda|enoda" to "என்னோட", "unnoda|unnuda|unoda" to "உன்னோட",
        // ---- question words
        "enna|ena|yenna|yena" to "என்ன", "ennada|enada|yenada" to "என்னடா",
        "ennachu|enachu|yenachu" to "என்னாச்சு", "yen|ean|yaen" to "ஏன்",
        "epdi|eppadi|yepdi|yeppadi|epadi" to "எப்படி", "enga|yenga" to "எங்க",
        "epo|eppo|yeppo|yepo" to "எப்போ", "yaaru|yaru" to "யாரு", "yaar|yar" to "யார்",
        "edhu|ethu|yethu|yedhu" to "எது", "ethukku|ethuku|edhuku|edhukku|yedhuku" to "எதுக்கு",
        "evlo|evvalavu|yevlo|evalo" to "எவ்வளவு", "ethana|yethana|ethanai" to "எத்தனை",
        // ---- this / that / here / there / time
        "idhu|ithu" to "இது", "adhu|athu" to "அது", "inga|ingae|ingey" to "இங்க", "anga|angae" to "அங்க",
        "ipo|ippo" to "இப்போ", "apo|appo" to "அப்போ", "ivlo|ivvalavu" to "இவ்வளவு", "avlo|avvalavu" to "அவ்வளவு",
        "inniku|innaiku|innaikku|innikku" to "இன்னைக்கு", "naalaiku|naalaikku|nalaiku|nalaikku" to "நாளைக்கு",
        "nethu|netru" to "நேத்து", "kaalai|kalai" to "காலை", "saayangalam|sayangalam" to "சாயங்காலம்",
        "raathiri|rathiri|rathri|raathri" to "ராத்திரி", "neram|nearam" to "நேரம்",
        "aprom|apram|appuram|apuram" to "அப்புறம்", "seekiram|sikiram|seekaram|sikkiram" to "சீக்கிரம்",
        // ---- particles / yes-no
        "ku|kku" to "க்கு", "la" to "ல", "um" to "உம்",
        "da|daa" to "டா", "di|dee" to "டி", "dei|dey|dai" to "டேய்", "machan|machaan" to "மச்சான்", "machi" to "மச்சி",
        "dhan|than|thaan|dhaan" to "தான்", "illa|ila" to "இல்ல", "illai|ilai" to "இல்லை",
        "illana|ilana|illanna" to "இல்லன்னா", "aama|ama|aamaa" to "ஆமா", "sari|seri" to "சரி",
        "ok|okay|okey" to "ஓகே", "kitta|kita" to "கிட்ட", "kooda|kuda" to "கூட",
        "ellam|elam|ellaam" to "எல்லாம்", "ellarum|elarum|ellaarum" to "எல்லாரும்",
        "yaarum|yarum" to "யாரும்", "onnum|onum" to "ஒன்னும்", "konjam|konja|koncham" to "கொஞ்சம்",
        "romba|rombo|rumba" to "ரொம்ப", "nallaa|nalaa" to "நல்லா", "kandippa|kandipa" to "கண்டிப்பா",
        "unmaiya|unmaiyaa" to "உண்மையா", "super|superu" to "சூப்பர்",
        // ---- need / can / know
        "venam|vendam|vendaam|venaam" to "வேணாம்", "venum|vendum|venuma" to "வேணும்",
        "mudiyathu|mudiyadhu|mudiyaadhu" to "முடியாது", "mudiyuma|mudiyumaa" to "முடியுமா",
        "theriyum|theriyu" to "தெரியும்", "theriyuma|theriyumaa" to "தெரியுமா",
        "theriyathu|theriyadhu|theriyaadhu" to "தெரியாது", "therila|theriyala|theriyale" to "தெரியல",
        "puriyuthu|puriyudhu" to "புரியுது", "puriyala|puriyale|purila" to "புரியல",
        // ---- to be
        "iruku|irukku" to "இருக்கு", "irukka" to "இருக்க", "irukiya|irukkiya" to "இருக்கியா",
        "irukken|iruken|irukkean" to "இருக்கேன்", "irukeenga|irukkeenga|irukinga|irukkinga" to "இருக்கீங்க",
        "irukeengala|irukkeengala|irukingala|irukkingala" to "இருக்கீங்களா",
        "irundhen|irunthen" to "இருந்தேன்", "irukkum|irukum" to "இருக்கும்",
        "irukanga|irukkanga|irukaanga|irukkaanga" to "இருக்காங்க", "irukaan|irukkaan|irukan" to "இருக்கான்",
        "iruka|irukaa|irukkaa" to "இருக்கா", "irunthuchu|irundhuchu" to "இருந்துச்சு",
        // ---- do
        "panra|panre|pandra" to "பண்ற", "panna" to "பண்ண", "pannura|panura" to "பண்ணுற", "panren|panuren|pannuren" to "பண்றேன்",
        "pannu|panu" to "பண்ணு", "panni|pani" to "பண்ணி", "pannunga|panunga" to "பண்ணுங்க",
        "pannala|panala|pannale" to "பண்ணல", "panniya|paniya" to "பண்ணியா",
        // ---- go / come
        "po|poo" to "போ", "poren|poaren|porean" to "போறேன்", "pore|pora" to "போற", "ponga" to "போங்க",
        "poi" to "போய்", "ponen|poanen|ponean" to "போனேன்", "pona" to "போன", "polam|polaam" to "போலாம்",
        "polama|polaama" to "போலாமா", "vaa|va" to "வா", "vaanga|vanga" to "வாங்க",
        "varen|vaaren|vaaran" to "வரேன்", "vara|vaara" to "வர", "varuviya|varuvia" to "வருவியா",
        "varuveengala|varuvingala" to "வருவீங்களா", "vandhen|vanthen" to "வந்தேன்", "vandha|vantha" to "வந்த",
        "vandhaan|vanthaan|vandhan|vanthan" to "வந்தான்", "vandhutten|vanthuten|vandhuten" to "வந்துட்டேன்",
        // ---- eat / drink / sleep
        "saptiya|saapitiya|sapitiya|saptiyaa|saaptiya|sapdiya" to "சாப்டியா",
        "saptingala|sapteengala|saapteengala|saaptingala" to "சாப்டீங்களா",
        "sapten|saapten|saptean" to "சாப்டேன்", "saapdu|sapdu" to "சாப்டு", "saapida|sapida|saapda" to "சாப்பிட",
        "saapadu|sapadu|saapaadu|sappadu" to "சாப்பாடு", "sapdalam|saapdalam|saapidalam" to "சாப்டலாம்",
        "thanni|tanni|thani" to "தண்ணி", "thoongu|thungu" to "தூங்கு", "thoongitten|thungiten" to "தூங்கிட்டேன்",
        // ---- say / see / give / take / talk
        "sollu|solu" to "சொல்லு", "sollunga|solunga" to "சொல்லுங்க", "sonnen|sonen" to "சொன்னேன்",
        "sonna|sona" to "சொன்ன", "paaru|paru" to "பாரு", "paathen|pathen|paarthen" to "பார்த்தேன்",
        "paathiya|pathiya" to "பாத்தியா", "paakalam|pakalam|paakkalam" to "பாக்கலாம்",
        "kudu|kodu" to "குடு", "kudunga|kodunga" to "குடுங்க", "edu|yedu" to "எடு",
        "vai|vei" to "வை", "vachu|vechu|vacchu" to "வச்சு", "pesu" to "பேசு", "pesalam|pesalaam" to "பேசலாம்",
        "kelu|kel" to "கேளு", "ketten|keten" to "கேட்டேன்",
        // ---- happened / done
        "aachu|achu" to "ஆச்சு", "aagum|agum" to "ஆகும்", "aagathu|agadhu|aagadhu" to "ஆகாது",
        "aayiduchu|ayiduchu|aayidichu" to "ஆயிடுச்சு", "mudinjiducha|mudinjudha|mudinjucha" to "முடிஞ்சுதா",
        // ---- family / places / things
        "amma" to "அம்மா", "appa" to "அப்பா", "anna" to "அண்ணா", "akka" to "அக்கா",
        "thambi|tambi" to "தம்பி", "thangachi|thangai" to "தங்கச்சி",
        "paiyan|payan" to "பையன்", "ponnu|ponu" to "பொண்ணு", "kuzhandhai|kolandha|kozhandha" to "குழந்தை",
        "veedu|vidu" to "வீடு", "veetuku|vetuku|veetukku|veettukku" to "வீட்டுக்கு", "veetla|vetla|veettula" to "வீட்டுல",
        "office|offc" to "ஆபீஸ்", "kadai|kada" to "கடை", "ooru|oor" to "ஊரு",
        "kaasu|kasu" to "காசு", "panam" to "பணம்", "velai|vela" to "வேலை", "kaapi|coffee" to "காபி", "tea" to "டீ",
        // ---- greetings / adjectives
        "vanakkam|vanakam" to "வணக்கம்", "nandri|nanri" to "நன்றி", "mannichidu|mannichu|manichu" to "மன்னிச்சு",
        "sorry" to "சாரி", "periya" to "பெரிய", "chinna|sinna" to "சின்ன", "pudhu|puthu" to "புது",
        "azhaga|alaga|azhagaa" to "அழகா",
    )

    private val map: Map<String, String> = buildMap {
        for ((keys, tamil) in words) for (k in keys.split('|')) put(k, tamil)
    }

    /** Tamil for a whole Tanglish word (letters only), or null if not in the dictionary. */
    fun lookup(word: String): String? = map[word.lowercase()]
}
