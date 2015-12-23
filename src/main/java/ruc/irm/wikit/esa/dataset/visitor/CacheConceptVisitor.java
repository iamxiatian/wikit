package ruc.irm.wikit.esa.dataset.visitor;

import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.domain.FullConcept;
import ruc.irm.wikit.esa.dataset.DatasetVisitor;

import java.io.IOException;

/**
 * Save Concept to Cache, so that we can access the concept by title or id quickly.
 *
 * User: xiatian
 * Date: 4/13/14
 * Time: 12:18 PM
 */
public class CacheConceptVisitor implements DatasetVisitor {
    private ConceptCache conceptCache = null;

    public CacheConceptVisitor(ConceptCache conceptCache) {
        this.conceptCache = conceptCache;
    }

    @Override
    public boolean filter(FullConcept concept) {
        //Save concept to cache
        conceptCache.saveCategories(concept.getId(), concept.getCategories());
        conceptCache.saveAlias(concept.getId(), concept.getAliasNames());
        conceptCache.saveOutId(concept.getId(), concept.getOutId());
        //save link relations
        for (String toName : concept.getOutLinks()) {
            int toId = conceptCache.getIdByNameOrAlias(toName, -1);
            if (toId >= 0) {
                conceptCache.saveLinkRelation(concept.getId(), toId);
            }
        }
        //conceptCache.incPageView(concept.getId(), 1);
        //conceptCache.saveSumOfPageViews();

        return true;
    }

    @Override
    public void close() throws IOException {
    }
}
