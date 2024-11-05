package at.tuwien.entities.identifier;

import at.tuwien.entities.database.LanguageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_identifier_titles")
public class IdentifierTitle implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "identifier-titles-sequence")
    @GenericGenerator(name = "identifier-titles-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "ENUM('ALTERNATIVE_TITLE', 'SUBTITLE', 'TRANSLATED_TITLE', 'OTHER')")
    @Enumerated(EnumType.STRING)
    private TitleType titleType;

    @Column(columnDefinition = "ENUM('AB','AA','AF','AK','SQ','AM','AR','AN','HY','AS','AV','AE','AY','AZ','BM','BA','EU','BE','BN','BH','BI','BS','BR','BG','MY','CA','KM','CH','CE','NY','ZH','CU','CV','KW','CO','CR','HR','CS','DA','DV','NL','DZ','EN','EO','ET','EE','FO','FJ','FI','FR','FF','GD','GL','LG','KA','DE','KI','EL','KL','GN','GU','HT','HA','HE','HZ','HI','HO','HU','IS','IO','IG','ID','IA','IE','IU','IK','GA','IT','JA','JV','KN','KR','KS','KK','RW','KV','KG','KO','KJ','KU','KY','LO','LA','LV','LB','LI','LN','LT','LU','MK','MG','MS','ML','MT','GV','MI','MR','MH','RO','MN','NA','NV','ND','NG','NE','SE','NO','NB','NN','II','OC','OJ','OR','OM','OS','PI','PA','PS','FA','PL','PT','QU','RM','RN','RU','SM','SG','SA','SC','SR','SN','SD','SI','SK','SL','SO','ST','NR','ES','SU','SW','SS','SV','TL','TY','TG','TA','TT','TE','TH','BO','TI','TO','TS','TN','TR','TK','TW','UG','UK','UR','UZ','VE','VI','VO','WA','CY','FY','WO','XH','YI','YO','ZA','ZU')")
    @Enumerated(EnumType.STRING)
    private LanguageType language;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "pid", referencedColumnName = "id", updatable = false)
    })
    private Identifier identifier;

}


