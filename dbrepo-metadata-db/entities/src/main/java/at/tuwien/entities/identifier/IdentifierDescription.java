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
@Table(name = "mdb_identifier_descriptions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"pid", "language"})
})
public class IdentifierDescription implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "identifier-descriptions-sequence")
    @GenericGenerator(name = "identifier-descriptions-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(columnDefinition = "enum('Abstract', 'Methods', 'SeriesInformation', 'TableOfContents', 'TechnicalInfo', 'Other')")
    @Enumerated(EnumType.STRING)
    private DescriptionType descriptionType;

    @Column(columnDefinition = "enum('ab','aa','af','ak','sq','am','ar','an','hy','as','av','ae','ay','az','bm','ba','eu','be','bn','bh','bi','bs','br','bg','my','ca','km','ch','ce','ny','zh','cu','cv','kw','co','cr','hr','cs','da','dv','nl','dz','en','eo','et','ee','fo','fj','fi','fr','ff','gd','gl','lg','ka','de','ki','el','kl','gn','gu','ht','ha','he','hz','hi','ho','hu','is','io','ig','id','ia','ie','iu','ik','ga','it','ja','jv','kn','kr','ks','kk','rw','kv','kg','ko','kj','ku','ky','lo','la','lv','lb','li','ln','lt','lu','mk','mg','ms','ml','mt','gv','mi','mr','mh','ro','mn','na','nv','nd','ng','ne','se','no','nb','nn','ii','oc','oj','or','om','os','pi','pa','ps','fa','pl','pt','qu','rm','rn','ru','sm','sg','sa','sc','sr','sn','sd','si','sk','sl','so','st','nr','es','su','sw','ss','sv','tl','ty','tg','ta','tt','te','th','bo','ti','to','ts','tn','tr','tk','tw','ug','uk','ur','uz','ve','vi','vo','wa','cy','fy','wo','xh','yi','yo','za','zu')")
    @Enumerated(EnumType.STRING)
    private LanguageType language;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "pid", referencedColumnName = "id", updatable = false)
    })
    private Identifier identifier;

}


