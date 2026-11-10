package iuh.fit.nhom16.pcstore.advisory.infrastructure;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import java.util.List;

/** Spring AI 2.0.2's PgVector SQL uses distance < limit, which excludes exact ties.
 * Widen by one double ULP, then enforce the original inclusive score threshold. */
public class InclusiveCosinePgVectorStore extends PgVectorStore {
    public InclusiveCosinePgVectorStore(PgVectorStoreBuilder builder) { super(builder); }
    @Override public List<Document> doSimilaritySearch(SearchRequest request) {
        double threshold=request.getSimilarityThreshold();
        SearchRequest inclusive=SearchRequest.from(request).similarityThreshold(Math.max(0,Math.nextDown(threshold))).build();
        return super.doSimilaritySearch(inclusive).stream().filter(document -> document.getScore()!=null
                && Double.isFinite(document.getScore()) && document.getScore()>=threshold).toList();
    }
}
