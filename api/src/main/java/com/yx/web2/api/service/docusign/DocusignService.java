package com.yx.web2.api.service.docusign;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.model.*;
import com.google.common.collect.Lists;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.config.DocusignConfig;
import com.yx.web2.api.config.Web2ApiConfig;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@Service
public class DocusignService {
    private final DocusignConfig docusignConfig;
    private final ApiClient apiClient;

    private volatile String AccessToken;
    private volatile String AccountId;

    private final Web2ApiConfig web2ApiConfig;

    private static final String CONSENT_REQUIRED_KEYWORD = "consent_required";

    public DocusignService(DocusignConfig docusignConfig, Web2ApiConfig web2ApiConfig) {
        this.docusignConfig = docusignConfig;
        this.web2ApiConfig = web2ApiConfig;
        apiClient = new ApiClient();
        apiClient.setBasePath(docusignConfig.getBaseUrl());
        apiClient.setOAuthBasePath(docusignConfig.getOAuthBasePath());
    }

    private void initOAuthAccessToken(boolean forceAuth) throws IOException, ApiException {
        if (!forceAuth) {
            if (AccessToken != null && AccountId != null) {
                return;
            }
        }
        byte[] privateKeyBytes = Files.readAllBytes(Paths.get(docusignConfig.getPrivateKeyLocation()));
        OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(
                docusignConfig.getClientId(),
                docusignConfig.getUserId(),
                Lists.newArrayList("signature"),
                privateKeyBytes,
                3600);
        String accessToken = oAuthToken.getAccessToken();
        OAuth.UserInfo userInfo = apiClient.getUserInfo(accessToken);
        apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
        AccessToken = accessToken;
        AccountId = userInfo.getAccounts().get(0).getAccountId();
    }

    public String sendContract(File tenantContractFile, boolean isFullContract, String contractId, String documentId, String documentName,
                               String emailSubject, String signerEmail, String signerName)
            throws DocuSignUnAuthorizationException, IOException, ApiException {
        return _sendContract(tenantContractFile, isFullContract, contractId, documentId, documentName, emailSubject, signerEmail, signerName, true);
    }

    public Envelope getEnvelope(String envelopeId) throws DocuSignUnAuthorizationException {
        return _getEnvelope(envelopeId, true);
    }

    public byte[] getDocument(String envelopeId, String documentId) throws DocuSignUnAuthorizationException {
        return _getDocument(envelopeId, documentId, true);
    }

    public String getEnvelopePreviewUrl(String envelopeId) throws DocuSignUnAuthorizationException {
        return _getEnvelopePreviewUrl(envelopeId, true);
    }

    private String _sendContract(File tenantContractFile, boolean isFullContract, String contractId, String documentId, String documentName,
                                 String emailSubject, String signerEmail, String signerName, boolean isRetry) throws DocuSignUnAuthorizationException, ApiException, IOException {
        try {
            initOAuthAccessToken(false);

            // Create envelopeDefinition object
            EnvelopeDefinition envelope = new EnvelopeDefinition();
            envelope.setEmailSubject(emailSubject);
            envelope.setStatus("sent");

            java.util.List<SignHere> signHereList = Lists.newArrayList();
            if (isFullContract) {
                SignHere signHere = new SignHere();
                signHere.setAnchorString(web2ApiConfig.getContract().getSignConfig().getFullSignOneAnchor());
                signHere.setAnchorXOffset(web2ApiConfig.getContract().getSignConfig().getFullSignOneAnchorXOffset());
                signHere.setAnchorYOffset(web2ApiConfig.getContract().getSignConfig().getFullSignOneAnchorYOffset());
                signHere.setAnchorUnits("pixels");
                signHere.setDocumentId(documentId);
                signHereList.add(signHere);

                SignHere signHere2 = new SignHere();
                signHere2.setAnchorString(web2ApiConfig.getContract().getSignConfig().getFullSignTwoAnchor());
                signHere2.setAnchorXOffset(web2ApiConfig.getContract().getSignConfig().getFullSignTwoAnchorXOffset());
                signHere2.setAnchorYOffset(web2ApiConfig.getContract().getSignConfig().getFullSignTwoAnchorYOffset());
                signHere2.setAnchorUnits("pixels");
                signHere2.setDocumentId(documentId);
                signHereList.add(signHere2);
            } else {
                SignHere signHere = new SignHere();
                signHere.setAnchorString(web2ApiConfig.getContract().getSignConfig().getSmartSignAnchor());
                signHere.setAnchorXOffset(web2ApiConfig.getContract().getSignConfig().getSmartSignAnchorXOffset());
                signHere.setAnchorYOffset(web2ApiConfig.getContract().getSignConfig().getSmartSignAnchorYOffset());
                signHere.setAnchorUnits("pixels");
                signHere.setDocumentId(documentId);
                signHereList.add(signHere);
            }
            Tabs tabs = new Tabs();
            tabs.setSignHereTabs(signHereList);

            // Set recipients
            Signer tenantSigner = new Signer();
            tenantSigner.setEmail(signerEmail);
            tenantSigner.setName(signerName);
            tenantSigner.recipientId("1");
            tenantSigner.setTabs(tabs);

            Recipients recipients = new Recipients();
            recipients.setSigners(Lists.newArrayList(tenantSigner));
            envelope.setRecipients(recipients);

            Document document = new Document();
            byte[] data = FileUtils.readFileToByteArray(tenantContractFile);
            document.setDocumentBase64(Base64.getEncoder().encodeToString(data));
            document.setName(documentName);
            document.setFileExtension("docx");
            document.setDocumentId(documentId);
            envelope.setDocuments(Lists.newArrayList(document));

            EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
            EnvelopeSummary results = envelopesApi.createEnvelope(AccountId, envelope);
            return results.getEnvelopeId();
        } catch (ApiException e) {
            if (e.getMessage().contains("USER_AUTHENTICATION_FAILED")) {
                if (!isRetry) {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_CREATE_ENVELOPE)
                            .p("DocumentId", documentId)
                            .p("ContractId", contractId)
                            .p(LogFieldConstants.ERR_MSG, e.getMessage())
                            .e(e);
                    return null;
                }
                try {
                    initOAuthAccessToken(true);
                    return _sendContract(tenantContractFile, isFullContract, contractId, documentId, documentName, emailSubject, signerEmail, signerName, false);
                } catch (Exception ex) {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_CREATE_ENVELOPE)
                            .p("DocumentId", documentId)
                            .p("ContractId", contractId)
                            .p(LogFieldConstants.ERR_MSG, e.getMessage())
                            .e(e);
                    return null;
                }
            }
            if (e.getMessage().contains(CONSENT_REQUIRED_KEYWORD)) {
                throw new DocuSignUnAuthorizationException();
            }
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_CREATE_ENVELOPE)
                    .p("DocumentId", documentId)
                    .p("ContractId", contractId)
                    .p(LogFieldConstants.ERR_MSG, e.getMessage())
                    .e(e);
            throw e;
        } catch (IOException e) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_CREATE_ENVELOPE)
                    .p("DocumentId", documentId)
                    .p("ContractId", contractId)
                    .p(LogFieldConstants.ERR_MSG, e.getMessage())
                    .e(e);
            throw e;
        }
    }

    private Envelope _getEnvelope(String envelopeId, boolean isRetry) throws DocuSignUnAuthorizationException {
        try {
            initOAuthAccessToken(false);
            EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
            return envelopesApi.getEnvelope(AccountId, envelopeId);
        } catch (ApiException e) {
            if (e.getMessage().contains("USER_AUTHENTICATION_FAILED")) {
                if (!isRetry) {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_GET_ENVELOPE)
                            .p("EnvelopeId", envelopeId)
                            .p(LogFieldConstants.ERR_MSG, e.getMessage())
                            .e(e);
                    return null;
                }
                try {
                    initOAuthAccessToken(true);
                    return _getEnvelope(envelopeId, false);
                } catch (Exception ex) {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_GET_ENVELOPE)
                            .p("EnvelopeId", envelopeId)
                            .p(LogFieldConstants.ERR_MSG, e.getMessage())
                            .e(e);
                    return null;
                }
            }
            if (e.getMessage().contains(CONSENT_REQUIRED_KEYWORD)) {
                throw new DocuSignUnAuthorizationException();
            }
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_GET_ENVELOPE)
                    .p("EnvelopeId", envelopeId)
                    .p(LogFieldConstants.ERR_MSG, e.getMessage())
                    .e(e);
            return null;
        } catch (IOException e) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_GET_ENVELOPE)
                    .p("EnvelopeId", envelopeId)
                    .p(LogFieldConstants.ERR_MSG, e.getMessage())
                    .e(e);
            return null;
        }
    }

    private byte[] _getDocument(String envelopeId, String documentId, boolean isRetry) throws DocuSignUnAuthorizationException {
        try {
            initOAuthAccessToken(false);
            EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
            return envelopesApi.getDocument(AccountId, envelopeId, documentId);
        } catch (ApiException e) {
            if (e.getMessage().contains("USER_AUTHENTICATION_FAILED")) {
                if (!isRetry) {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_GET_DOCUMENT)
                            .p("EnvelopeId", envelopeId)
                            .p(LogFieldConstants.ERR_MSG, e.getMessage())
                            .e(e);
                    return null;
                }
                try {
                    initOAuthAccessToken(true);
                    return _getDocument(envelopeId, documentId, false);
                } catch (Exception ex) {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_GET_DOCUMENT)
                            .p("envelopeId", envelopeId)
                            .p("errMsg", e.getMessage())
                            .e(e);
                    return null;
                }
            }
            if (e.getMessage().contains(CONSENT_REQUIRED_KEYWORD)) {
                throw new DocuSignUnAuthorizationException();
            }
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_GET_DOCUMENT)
                    .p("envelopeId", envelopeId)
                    .p("errMsg", e.getMessage())
                    .e(e);
            return null;
        } catch (IOException e) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_GET_DOCUMENT)
                    .p("envelopeId", envelopeId)
                    .p("errMsg", e.getMessage())
                    .e(e);
            return null;
        }
    }

    private String _getEnvelopePreviewUrl(String envelopeId, boolean isRetry) throws DocuSignUnAuthorizationException {
        try {
            initOAuthAccessToken(false);
            EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);

            ConsoleViewRequest request = new ConsoleViewRequest();
            request.setEnvelopeId(envelopeId);
            ViewUrl viewUrlApiResponse = envelopesApi.createConsoleView(AccountId, request);
            return viewUrlApiResponse.getUrl();
        } catch (ApiException e) {
            if (e.getMessage().contains("USER_AUTHENTICATION_FAILED")) {
                if (!isRetry) {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_GET_ENVELOPE_PREVIEW_URL)
                            .p("envelopeId", envelopeId)
                            .p("errMsg", e.getMessage())
                            .e(e);
                    return null;
                }
                try {
                    initOAuthAccessToken(true);
                    return _getEnvelopePreviewUrl(envelopeId, false);
                } catch (Exception ex) {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_GET_ENVELOPE_PREVIEW_URL)
                            .p("envelopeId", envelopeId)
                            .p("errMsg", e.getMessage())
                            .e(e);
                    return null;
                }
            }
            if (e.getMessage().contains(CONSENT_REQUIRED_KEYWORD)) {
                throw new DocuSignUnAuthorizationException();
            }
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_GET_ENVELOPE_PREVIEW_URL)
                    .p("envelopeId", envelopeId)
                    .p("errMsg", e.getMessage())
                    .e(e);
            return null;
        } catch (IOException e) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_GET_ENVELOPE_PREVIEW_URL)
                    .p("envelopeId", envelopeId)
                    .p("errMsg", e.getMessage())
                    .e(e);
            return null;
        }
    }
}
