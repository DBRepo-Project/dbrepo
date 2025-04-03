package at.ac.tuwien.ifs.dbrepo.core.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum LanguageTypeDto {

    @JsonProperty("ab")
    AB("ab"),

    @JsonProperty("aa")
    AA("aa"),

    @JsonProperty("af")
    AF("af"),

    @JsonProperty("ak")
    AK("ak"),

    @JsonProperty("sq")
    SQ("sq"),

    @JsonProperty("am")
    AM("am"),

    @JsonProperty("ar")
    AR("ar"),

    @JsonProperty("an")
    AN("an"),

    @JsonProperty("hy")
    HY("hy"),

    @JsonProperty("as")
    AS("as"),

    @JsonProperty("av")
    AV("av"),

    @JsonProperty("ae")
    AE("ae"),

    @JsonProperty("ay")
    AY("ay"),

    @JsonProperty("az")
    AZ("az"),

    @JsonProperty("bm")
    BM("bm"),

    @JsonProperty("ba")
    BA("ba"),

    @JsonProperty("eu")
    EU("eu"),

    @JsonProperty("be")
    BE("be"),

    @JsonProperty("bn")
    BN("bn"),

    @JsonProperty("bh")
    BH("bh"),

    @JsonProperty("bi")
    BI("bi"),

    @JsonProperty("bs")
    BS("bs"),

    @JsonProperty("br")
    BR("br"),

    @JsonProperty("bg")
    BG("bg"),

    @JsonProperty("my")
    MY("my"),

    @JsonProperty("ca")
    CA("ca"),

    @JsonProperty("km")
    KM("km"),

    @JsonProperty("ch")
    CH("ch"),

    @JsonProperty("ce")
    CE("ce"),

    @JsonProperty("ny")
    NY("ny"),

    @JsonProperty("zh")
    ZH("zh"),

    @JsonProperty("cu")
    CU("cu"),

    @JsonProperty("cv")
    CV("cv"),

    @JsonProperty("kw")
    KW("kw"),

    @JsonProperty("co")
    CO("co"),

    @JsonProperty("cr")
    CR("cr"),

    @JsonProperty("hr")
    HR("hr"),

    @JsonProperty("cs")
    CS("cs"),

    @JsonProperty("da")
    DA("da"),

    @JsonProperty("dv")
    DV("dv"),

    @JsonProperty("nl")
    NL("nl"),

    @JsonProperty("dz")
    DZ("dz"),

    @JsonProperty("en")
    EN("en"),

    @JsonProperty("eo")
    EO("eo"),

    @JsonProperty("et")
    ET("et"),

    @JsonProperty("ee")
    EE("ee"),

    @JsonProperty("fo")
    FO("fo"),

    @JsonProperty("fj")
    FJ("fj"),

    @JsonProperty("fi")
    FI("fi"),

    @JsonProperty("fr")
    FR("fr"),

    @JsonProperty("ff")
    FF("ff"),

    @JsonProperty("gd")
    GD("gd"),

    @JsonProperty("gl")
    GL("gl"),

    @JsonProperty("lg")
    LG("lg"),

    @JsonProperty("ka")
    KA("ka"),

    @JsonProperty("de")
    DE("de"),

    @JsonProperty("ki")
    KI("ki"),

    @JsonProperty("el")
    EL("el"),

    @JsonProperty("kl")
    KL("kl"),

    @JsonProperty("gn")
    GN("gn"),

    @JsonProperty("gu")
    GU("gu"),

    @JsonProperty("ht")
    HT("ht"),

    @JsonProperty("ha")
    HA("ha"),

    @JsonProperty("he")
    HE("he"),

    @JsonProperty("hz")
    HZ("hz"),

    @JsonProperty("hi")
    HI("hi"),

    @JsonProperty("ho")
    HO("ho"),

    @JsonProperty("hu")
    HU("hu"),

    @JsonProperty("is")
    IS("is"),

    @JsonProperty("io")
    IO("io"),

    @JsonProperty("ig")
    IG("ig"),

    @JsonProperty("id")
    ID("id"),

    @JsonProperty("ia")
    IA("ia"),

    @JsonProperty("ie")
    IE("ie"),

    @JsonProperty("iu")
    IU("iu"),

    @JsonProperty("ik")
    IK("ik"),

    @JsonProperty("ga")
    GA("ga"),

    @JsonProperty("it")
    IT("it"),

    @JsonProperty("ja")
    JA("ja"),

    @JsonProperty("jv")
    JV("jv"),

    @JsonProperty("kn")
    KN("kn"),

    @JsonProperty("kr")
    KR("kr"),

    @JsonProperty("ks")
    KS("ks"),

    @JsonProperty("kk")
    KK("kk"),

    @JsonProperty("rw")
    RW("rw"),

    @JsonProperty("kv")
    KV("kv"),

    @JsonProperty("kg")
    KG("kg"),

    @JsonProperty("ko")
    KO("ko"),

    @JsonProperty("kj")
    KJ("kj"),

    @JsonProperty("ku")
    KU("ku"),

    @JsonProperty("ky")
    KY("ky"),

    @JsonProperty("lo")
    LO("lo"),

    @JsonProperty("la")
    LA("la"),

    @JsonProperty("lv")
    LV("lv"),

    @JsonProperty("lb")
    LB("lb"),

    @JsonProperty("li")
    LI("li"),

    @JsonProperty("ln")
    LN("ln"),

    @JsonProperty("lt")
    LT("lt"),

    @JsonProperty("lu")
    LU("lu"),

    @JsonProperty("mk")
    MK("mk"),

    @JsonProperty("mg")
    MG("mg"),

    @JsonProperty("ms")
    MS("ms"),

    @JsonProperty("ml")
    ML("ml"),

    @JsonProperty("mt")
    MT("mt"),

    @JsonProperty("gv")
    GV("gv"),

    @JsonProperty("mi")
    MI("mi"),

    @JsonProperty("mr")
    MR("mr"),

    @JsonProperty("mh")
    MH("mh"),

    @JsonProperty("ro")
    RO("ro"),

    @JsonProperty("mn")
    MN("mn"),

    @JsonProperty("na")
    NA("na"),

    @JsonProperty("nv")
    NV("nv"),

    @JsonProperty("nd")
    ND("nd"),

    @JsonProperty("ng")
    NG("ng"),

    @JsonProperty("ne")
    NE("ne"),

    @JsonProperty("se")
    SE("se"),

    @JsonProperty("no")
    NO("no"),

    @JsonProperty("nb")
    NB("nb"),

    @JsonProperty("nn")
    NN("nn"),

    @JsonProperty("ii")
    II("ii"),

    @JsonProperty("oc")
    OC("oc"),

    @JsonProperty("oj")
    OJ("oj"),

    @JsonProperty("or")
    OR("or"),

    @JsonProperty("om")
    OM("om"),

    @JsonProperty("os")
    OS("os"),

    @JsonProperty("pi")
    PI("pi"),

    @JsonProperty("pa")
    PA("pa"),

    @JsonProperty("ps")
    PS("ps"),

    @JsonProperty("fa")
    FA("fa"),

    @JsonProperty("pl")
    PL("pl"),

    @JsonProperty("pt")
    PT("pt"),

    @JsonProperty("qu")
    QU("qu"),

    @JsonProperty("rm")
    RM("rm"),

    @JsonProperty("rn")
    RN("rn"),

    @JsonProperty("ru")
    RU("ru"),

    @JsonProperty("sm")
    SM("sm"),

    @JsonProperty("sg")
    SG("sg"),

    @JsonProperty("sa")
    SA("sa"),

    @JsonProperty("sc")
    SC("sc"),

    @JsonProperty("sr")
    SR("sr"),

    @JsonProperty("sn")
    SN("sn"),

    @JsonProperty("sd")
    SD("sd"),

    @JsonProperty("si")
    SI("si"),

    @JsonProperty("sk")
    SK("sk"),

    @JsonProperty("sl")
    SL("sl"),

    @JsonProperty("so")
    SO("so"),

    @JsonProperty("st")
    ST("st"),

    @JsonProperty("nr")
    NR("nr"),

    @JsonProperty("es")
    ES("es"),

    @JsonProperty("su")
    SU("su"),

    @JsonProperty("sw")
    SW("sw"),

    @JsonProperty("ss")
    SS("ss"),

    @JsonProperty("sv")
    SV("sv"),

    @JsonProperty("tl")
    TL("tl"),

    @JsonProperty("ty")
    TY("ty"),

    @JsonProperty("tg")
    TG("tg"),

    @JsonProperty("ta")
    TA("ta"),

    @JsonProperty("tt")
    TT("tt"),

    @JsonProperty("te")
    TE("te"),

    @JsonProperty("th")
    TH("th"),

    @JsonProperty("bo")
    BO("bo"),

    @JsonProperty("ti")
    TI("ti"),

    @JsonProperty("to")
    TO("to"),

    @JsonProperty("ts")
    TS("ts"),

    @JsonProperty("tn")
    TN("tn"),

    @JsonProperty("tr")
    TR("tr"),

    @JsonProperty("tk")
    TK("tk"),

    @JsonProperty("tw")
    TW("tw"),

    @JsonProperty("ug")
    UG("ug"),

    @JsonProperty("uk")
    UK("uk"),

    @JsonProperty("ur")
    UR("ur"),

    @JsonProperty("uz")
    UZ("uz"),

    @JsonProperty("ve")
    VE("ve"),

    @JsonProperty("vi")
    VI("vi"),

    @JsonProperty("vo")
    VO("vo"),

    @JsonProperty("wa")
    WA("wa"),

    @JsonProperty("cy")
    CY("cy"),

    @JsonProperty("fy")
    FY("fy"),

    @JsonProperty("wo")
    WO("wo"),

    @JsonProperty("xh")
    XH("xh"),

    @JsonProperty("yi")
    YI("yi"),

    @JsonProperty("yo")
    YO("yo"),

    @JsonProperty("za")
    ZA("za"),

    @JsonProperty("zu")
    ZU("zu");

    private final String value;

    LanguageTypeDto(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
