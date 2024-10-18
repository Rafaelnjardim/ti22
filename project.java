import com.microsoft.azure.cognitiveservices.vision.faceapi.FaceAPI;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.DetectedFace;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.SimilarFace;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.DetectionModel;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.RecognitionModel;
import com.microsoft.azure.cognitiveservices.vision.faceapi.implementation.FaceAPIImpl;
import com.microsoft.azure.cognitiveservices.vision.faceapi.FaceClient;
import com.microsoft.azure.cognitiveservices.vision.faceapi.FaceClientImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Main {
    private static final String RECOGNITION_MODEL3 = RecognitionModel.RECOGNITION_03;

    public static FaceClient authenticate(String endpoint, String key) {
        return new FaceClientImpl(endpoint, key);
    }

    private static CompletableFuture<List<DetectedFace>> detectFaceRecognize(FaceClient faceClient, String url, String recognitionModel) {
        return faceClient.faces().detectWithUrlAsync(url, recognitionModel, DetectionModel.DETECTION_02)
                .thenApply(detectedFaces -> {
                    System.out.println(detectedFaces.size() + " face(s) detectada(s) na imagem `" + url + "`");
                    return detectedFaces;
                });
    }

    public static CompletableFuture<Void> findSimilar(FaceClient client, String recognitionModel) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                System.out.println("========Achar Similares========");
                System.out.println("Insira o link da face base (links muito grandes podem causar erros):");
                String sourceImageFileName = reader.readLine();

                System.out.println("Insira o link das possíveis faces similares e digite FIM quando acabar (links muito grandes podem causar erros):");
                List<String> targetImageFileNames = new ArrayList<>();
                String aux = "";
                int i = 1;
                do {
                    System.out.println("Imagem " + i + ":");
                    aux = reader.readLine();
                    if (!aux.equalsIgnoreCase("FIM")) {
                        targetImageFileNames.add(aux);
                    }
                    i++;
                } while (!aux.equalsIgnoreCase("FIM"));

                List<UUID> targetFaceIds = new ArrayList<>();
                for (String targetImageFileName : targetImageFileNames) {
                    List<DetectedFace> faces = detectFaceRecognize(client, targetImageFileName, recognitionModel).join();
                    targetFaceIds.add(faces.get(0).faceId());
                }

                List<DetectedFace> detectedFaces = detectFaceRecognize(client, sourceImageFileName, recognitionModel).join();
                UUID sourceFaceId = detectedFaces.get(0).faceId();

                List<SimilarFace> similarResults = client.faces().findSimilarAsync(sourceFaceId, targetFaceIds).join();
                i = 1;
                for (SimilarFace similarResult : similarResults) {
                    System.out.println("A imagem " + i + " com o FaceID: " + similarResult.faceId() +
                            " é similar à imagem base com confiança: " + similarResult.confidence() + ".");
                    i++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Insira a URL da sua aplicação no Azure:");
            String urlServico = reader.readLine();

            System.out.println("Insira a chave da sua aplicação no Azure:");
            String chaveServico = reader.readLine();

            FaceClient client = authenticate(urlServico, chaveServico);
            findSimilar(client, RECOGNITION_MODEL3).join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
