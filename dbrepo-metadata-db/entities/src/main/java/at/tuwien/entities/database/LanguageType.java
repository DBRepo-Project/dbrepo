package at.tuwien.entities.database;

import lombok.Getter;

@Getter
public enum LanguageType {

    AB("ab"),

    AA("aa"),

    AF("af"),

    AK("ak"),

    SQ("sq"),

    AM("am"),

    AR("ar"),

    AN("an"),

    HY("hy"),

    AS("as"),

    AV("av"),

    AE("ae"),

    AY("ay"),

    AZ("az"),

    BM("bm"),

    BA("ba"),

    EU("eu"),

    BE("be"),

    BN("bn"),

    BH("bh"),

    BI("bi"),

    BS("bs"),

    BR("br"),

    BG("bg"),

    MY("my"),

    CA("ca"),

    KM("km"),

    CH("ch"),

    CE("ce"),

    NY("ny"),

    ZH("zh"),

    CU("cu"),

    CV("cv"),

    KW("kw"),

    CO("co"),

    CR("cr"),

    HR("hr"),

    CS("cs"),

    DA("da"),

    DV("dv"),

    NL("nl"),

    DZ("dz"),

    EN("en"),

    EO("eo"),

    ET("et"),

    EE("ee"),

    FO("fo"),

    FJ("fj"),

    FI("fi"),

    FR("fr"),

    FF("ff"),

    GD("gd"),

    GL("gl"),

    LG("lg"),

    KA("ka"),

    DE("de"),

    KI("ki"),

    EL("el"),

    KL("kl"),

    GN("gn"),

    GU("gu"),

    HT("ht"),

    HA("ha"),

    HE("he"),

    HZ("hz"),

    HI("hi"),

    HO("ho"),

    HU("hu"),

    IS("is"),

    IO("io"),

    IG("ig"),

    ID("id"),

    IA("ia"),

    IE("ie"),

    IU("iu"),

    IK("ik"),

    GA("ga"),

    IT("it"),

    JA("ja"),

    JV("jv"),

    KN("kn"),

    KR("kr"),

    KS("ks"),

    KK("kk"),

    RW("rw"),

    KV("kv"),

    KG("kg"),

    KO("ko"),

    KJ("kj"),

    KU("ku"),

    KY("ky"),

    LO("lo"),

    LA("la"),

    LV("lv"),

    LB("lb"),

    LI("li"),

    LN("ln"),

    LT("lt"),

    LU("lu"),

    MK("mk"),

    MG("mg"),

    MS("ms"),

    ML("ml"),

    MT("mt"),

    GV("gv"),

    MI("mi"),

    MR("mr"),

    MH("mh"),

    RO("ro"),

    MN("mn"),

    NA("na"),

    NV("nv"),

    ND("nd"),

    NG("ng"),

    NE("ne"),

    SE("se"),

    NO("no"),

    NB("nb"),

    NN("nn"),

    II("ii"),

    OC("oc"),

    OJ("oj"),

    OR("or"),

    OM("om"),

    OS("os"),

    PI("pi"),

    PA("pa"),

    PS("ps"),

    FA("fa"),

    PL("pl"),

    PT("pt"),

    QU("qu"),

    RM("rm"),

    RN("rn"),

    RU("ru"),

    SM("sm"),

    SG("sg"),

    SA("sa"),

    SC("sc"),

    SR("sr"),

    SN("sn"),

    SD("sd"),

    SI("si"),

    SK("sk"),

    SL("sl"),

    SO("so"),

    ST("st"),

    NR("nr"),

    ES("es"),

    SU("su"),

    SW("sw"),

    SS("ss"),

    SV("sv"),

    TL("tl"),

    TY("ty"),

    TG("tg"),

    TA("ta"),

    TT("tt"),

    TE("te"),

    TH("th"),

    BO("bo"),

    TI("ti"),

    TO("to"),

    TS("ts"),

    TN("tn"),

    TR("tr"),

    TK("tk"),

    TW("tw"),

    UG("ug"),

    UK("uk"),

    UR("ur"),

    UZ("uz"),

    VE("ve"),

    VI("vi"),

    VO("vo"),

    WA("wa"),

    CY("cy"),

    FY("fy"),

    WO("wo"),

    XH("xh"),

    YI("yi"),

    YO("yo"),

    ZA("za"),

    ZU("zu");

    private String name;

    LanguageType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
