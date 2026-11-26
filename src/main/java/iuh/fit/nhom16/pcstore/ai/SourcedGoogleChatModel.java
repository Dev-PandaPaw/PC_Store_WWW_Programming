package iuh.fit.nhom16.pcstore.ai;

import com.google.genai.Client;
import com.google.genai.types.Candidate;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import java.util.*;

/** Preserve provider grounding citations that Spring AI 2.0.2 otherwise drops. Inference stays in ChatClient. */
final class SourcedGoogleChatModel extends GoogleGenAiChatModel {
    SourcedGoogleChatModel(Client client, GoogleGenAiChatOptions options) {
        super(client,options,ToolCallingManager.builder().build(),
                new RetryTemplate(RetryPolicy.builder().maxRetries(0).build()),ObservationRegistry.NOOP);
    }
    @Override protected List<Generation> responseCandidateToGeneration(Candidate candidate) {
        List<AiGateway.Source> sources = candidate.groundingMetadata()
                .flatMap(metadata -> metadata.groundingChunks()).orElse(List.of()).stream()
                .flatMap(chunk -> chunk.web().stream())
                .filter(web -> web.uri().isPresent())
                .map(web -> new AiGateway.Source(web.title().orElse("Nguồn yêu cầu cấu hình"),web.uri().orElseThrow()))
                .filter(source -> AiGateway.safeSource(source.url())).distinct().limit(8).toList();
        return super.responseCandidateToGeneration(candidate).stream().map(generation -> {
            var properties = new HashMap<String,Object>(generation.getOutput().getMetadata());
            properties.put("groundingSources",sources);
            return new Generation(generation.getOutput().mutate().properties(properties).build(),generation.getMetadata());
        }).toList();
    }
}
