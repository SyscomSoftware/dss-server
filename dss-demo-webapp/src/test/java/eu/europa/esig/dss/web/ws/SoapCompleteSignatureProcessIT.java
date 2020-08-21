package eu.europa.esig.dss.web.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore.PasswordProtection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDigestMatcher;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSignature;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.web.config.CXFConfig;
import eu.europa.esig.dss.ws.converter.DTOConverter;
import eu.europa.esig.dss.ws.dto.RemoteCertificate;
import eu.europa.esig.dss.ws.dto.RemoteDocument;
import eu.europa.esig.dss.ws.dto.SignatureValueDTO;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;
import eu.europa.esig.dss.ws.server.signing.dto.RemoteKeyEntry;
import eu.europa.esig.dss.ws.server.signing.soap.client.SoapSignatureTokenConnection;
import eu.europa.esig.dss.ws.signature.dto.DataToSignMultipleDocumentsDTO;
import eu.europa.esig.dss.ws.signature.dto.DataToSignOneDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.ExtendDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.SignMultipleDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.SignOneDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteSignatureParameters;
import eu.europa.esig.dss.ws.signature.soap.client.DateAdapter;
import eu.europa.esig.dss.ws.signature.soap.client.SoapDocumentSignatureService;
import eu.europa.esig.dss.ws.signature.soap.client.SoapMultipleDocumentsSignatureService;
import eu.europa.esig.dss.ws.validation.dto.DataToValidateDTO;
import eu.europa.esig.dss.ws.validation.dto.WSReportsDTO;
import eu.europa.esig.dss.ws.validation.soap.client.SoapDocumentValidationService;

public class SoapCompleteSignatureProcessIT extends AbstractIT {

	private SoapDocumentSignatureService soapClient;
	private SoapMultipleDocumentsSignatureService soapMultiDocsClient;
	private SoapSignatureTokenConnection soapServerSigning;
	private SoapDocumentValidationService soapValidationService;

	@BeforeEach
	public void init() {

		JAXBDataBinding dataBinding = new JAXBDataBinding();
		dataBinding.getConfiguredXmlAdapters().add(new DateAdapter());

		Map<String, Object> props = new HashMap<String, Object>();
		props.put("mtom-enabled", Boolean.TRUE);

		JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
		factory.setServiceClass(SoapDocumentSignatureService.class);
		factory.setProperties(props);
		factory.setDataBinding(dataBinding);
		factory.setAddress(getBaseCxf() + CXFConfig.SOAP_SIGNATURE_ONE_DOCUMENT);

		LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
		factory.getInInterceptors().add(loggingInInterceptor);
		factory.getInFaultInterceptors().add(loggingInInterceptor);

		LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
		factory.getOutInterceptors().add(loggingOutInterceptor);
		factory.getOutFaultInterceptors().add(loggingOutInterceptor);

		soapClient = factory.create(SoapDocumentSignatureService.class);

		
		dataBinding = new JAXBDataBinding();
		dataBinding.getConfiguredXmlAdapters().add(new DateAdapter());

		factory = new JaxWsProxyFactoryBean();
		factory.setServiceClass(SoapMultipleDocumentsSignatureService.class);
		factory.setProperties(props);
		factory.setDataBinding(dataBinding);
		factory.setAddress(getBaseCxf() + CXFConfig.SOAP_SIGNATURE_MULTIPLE_DOCUMENTS);

		loggingInInterceptor = new LoggingInInterceptor();
		factory.getInInterceptors().add(loggingInInterceptor);
		factory.getInFaultInterceptors().add(loggingInInterceptor);

		loggingOutInterceptor = new LoggingOutInterceptor();
		factory.getOutInterceptors().add(loggingOutInterceptor);
		factory.getOutFaultInterceptors().add(loggingOutInterceptor);

		soapMultiDocsClient = factory.create(SoapMultipleDocumentsSignatureService.class);
		

		factory = new JaxWsProxyFactoryBean();
		factory.setServiceClass(SoapSignatureTokenConnection.class);

		factory.setProperties(props);

		factory.setAddress(getBaseCxf() + CXFConfig.SOAP_SERVER_SIGNING);

		loggingInInterceptor = new LoggingInInterceptor();
		factory.getInInterceptors().add(loggingInInterceptor);
		factory.getInFaultInterceptors().add(loggingInInterceptor);

		loggingOutInterceptor = new LoggingOutInterceptor();
		factory.getOutInterceptors().add(loggingOutInterceptor);
		factory.getOutFaultInterceptors().add(loggingOutInterceptor);

		soapServerSigning = factory.create(SoapSignatureTokenConnection.class);
		
		
		factory = new JaxWsProxyFactoryBean();
		factory.setServiceClass(SoapDocumentValidationService.class);

		factory.setAddress(getBaseCxf() + CXFConfig.SOAP_VALIDATION);

		loggingInInterceptor = new LoggingInInterceptor();
		factory.getInInterceptors().add(loggingInInterceptor);
		factory.getInFaultInterceptors().add(loggingInInterceptor);

		loggingOutInterceptor = new LoggingOutInterceptor();
		factory.getOutInterceptors().add(loggingOutInterceptor);
		factory.getOutFaultInterceptors().add(loggingOutInterceptor);

		soapValidationService = factory.create(SoapDocumentValidationService.class);
	}

	@Test
	public void testSignExtendValidate() throws Exception {
		try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(new FileInputStream("src/test/resources/user_a_rsa.p12"),
				new PasswordProtection("password".toCharArray()))) {

			List<DSSPrivateKeyEntry> keys = token.getKeys();
			DSSPrivateKeyEntry dssPrivateKeyEntry = keys.get(0);

			RemoteSignatureParameters parameters = new RemoteSignatureParameters();
			parameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);
			parameters.setSigningCertificate(new RemoteCertificate(dssPrivateKeyEntry.getCertificate().getCertificate().getEncoded()));
			parameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
			parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);

			DSSDocument fileToSign = new InMemoryDocument("Hello World!".getBytes());
			RemoteDocument toSignDocument = new RemoteDocument(Utils.toByteArray(fileToSign.openStream()), fileToSign.getName());
			ToBeSignedDTO dataToSign = soapClient.getDataToSign(new DataToSignOneDocumentDTO(toSignDocument, parameters));
			assertNotNull(dataToSign);

			SignatureValue signatureValue = token.sign(DTOConverter.toToBeSigned(dataToSign), DigestAlgorithm.SHA256, dssPrivateKeyEntry);
			SignOneDocumentDTO signDocument = new SignOneDocumentDTO(toSignDocument, parameters,
					new SignatureValueDTO(signatureValue.getAlgorithm(), signatureValue.getValue()));
			RemoteDocument signedDocument = soapClient.signDocument(signDocument);

			assertNotNull(signedDocument);

			parameters = new RemoteSignatureParameters();
			parameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_T);

			RemoteDocument extendedDocument = soapClient.extendDocument(new ExtendDocumentDTO(signedDocument, parameters));

			DataToValidateDTO toValidate = new DataToValidateDTO(extendedDocument, (RemoteDocument) null, null);

			WSReportsDTO result = soapValidationService.validateSignature(toValidate);

			assertNotNull(result.getDiagnosticData());
			assertNotNull(result.getDetailedReport());
			assertNotNull(result.getSimpleReport());
			assertNotNull(result.getValidationReport());

			assertEquals(1, result.getSimpleReport().getSignatureOrTimestamp().size());
			
			XmlSignature xmlSignature = result.getDiagnosticData().getSignatures().get(0);
			assertEquals(1, xmlSignature.getFoundTimestamps().size());
			assertEquals(SignatureLevel.CAdES_BASELINE_T, xmlSignature.getSignatureFormat());
			assertEquals(result.getSimpleReport().getSignatureOrTimestamp().get(0).getIndication(), Indication.INDETERMINATE);
		}
	}
	
	@Test
	public void testServerSignExtendAndValidate() throws Exception {
		// extract the signing key
		List<RemoteKeyEntry> keys = soapServerSigning.getKeys();
		assertTrue(Utils.isCollectionNotEmpty(keys));
		
		RemoteKeyEntry remoteKeyEntry = keys.get(0);

		String alias = remoteKeyEntry.getAlias();

		// prepare the document to sign
		DSSDocument fileToSign = new InMemoryDocument("Hello World!".getBytes());
		RemoteDocument toSignDocument = new RemoteDocument(Utils.toByteArray(fileToSign.openStream()), fileToSign.getName());

		// define signature parameters
		RemoteSignatureParameters parameters = new RemoteSignatureParameters();
		parameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);
		parameters.setSigningCertificate(remoteKeyEntry.getCertificate());
		parameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
		parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);

		// get data to be signed
		ToBeSignedDTO toBeSigned = soapClient.getDataToSign(new DataToSignOneDocumentDTO(toSignDocument, parameters));

		// compute signature value with server signing
		SignatureValueDTO signatureValue = soapServerSigning.sign(toBeSigned, DigestAlgorithm.SHA256, alias);
		assertNotNull(signatureValue);
		assertNotNull(signatureValue.getAlgorithm());
		assertNotNull(signatureValue.getValue());
		
		// sign the document
		SignOneDocumentDTO signDocument = new SignOneDocumentDTO(toSignDocument, parameters, signatureValue);
		RemoteDocument signedDocument = soapClient.signDocument(signDocument);
		
		parameters = new RemoteSignatureParameters();
		parameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_T);

		// extend the document
		RemoteDocument extendedDocument = soapClient.extendDocument(new ExtendDocumentDTO(signedDocument, parameters));

		DataToValidateDTO toValidate = new DataToValidateDTO(extendedDocument, (RemoteDocument) null, null);

		// validate the document
		WSReportsDTO result = soapValidationService.validateSignature(toValidate);

		assertNotNull(result.getDiagnosticData());
		assertNotNull(result.getDetailedReport());
		assertNotNull(result.getSimpleReport());
		assertNotNull(result.getValidationReport());

		assertEquals(1, result.getSimpleReport().getSignatureOrTimestamp().size());
		
		XmlSignature xmlSignature = result.getDiagnosticData().getSignatures().get(0);
		assertEquals(1, xmlSignature.getFoundTimestamps().size());
		assertEquals(SignatureLevel.CAdES_BASELINE_T, xmlSignature.getSignatureFormat());
		assertEquals(DSSUtils.loadCertificate(remoteKeyEntry.getCertificate().getEncodedCertificate()).getDSSIdAsString(), 
				xmlSignature.getSigningCertificate().getCertificate().getId());
		assertEquals(result.getSimpleReport().getSignatureOrTimestamp().get(0).getIndication(), Indication.INDETERMINATE);
		
	}

	@Test
	public void jadesSignMultipleAndValidateTest() throws Exception {
		try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(new FileInputStream("src/test/resources/user_a_rsa.p12"),
				new PasswordProtection("password".toCharArray()))) {

			List<DSSPrivateKeyEntry> keys = token.getKeys();
			DSSPrivateKeyEntry dssPrivateKeyEntry = keys.get(0);

			RemoteSignatureParameters parameters = new RemoteSignatureParameters();
			parameters.setSignatureLevel(SignatureLevel.JAdES_BASELINE_B);
			parameters.setSigningCertificate(new RemoteCertificate(dssPrivateKeyEntry.getCertificate().getCertificate().getEncoded()));
			parameters.setSignaturePackaging(SignaturePackaging.DETACHED);
			parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
			
			FileDocument fileToSign = new FileDocument(new File("src/test/resources/sample.xml"));
			RemoteDocument toSignDocument = new RemoteDocument(DSSUtils.toByteArray(fileToSign), fileToSign.getName());
			RemoteDocument toSignDoc2 = new RemoteDocument("Hello world!".getBytes("UTF-8"), "test.bin");
			List<RemoteDocument> toSignDocuments = new ArrayList<RemoteDocument>();
			toSignDocuments.add(toSignDocument);
			toSignDocuments.add(toSignDoc2);
			ToBeSignedDTO dataToSign = soapMultiDocsClient.getDataToSign(new DataToSignMultipleDocumentsDTO(toSignDocuments, parameters));

			SignatureValue signatureValue = token.sign(DTOConverter.toToBeSigned(dataToSign), DigestAlgorithm.SHA256, dssPrivateKeyEntry);
			SignMultipleDocumentDTO signDocument = new SignMultipleDocumentDTO(toSignDocuments, parameters,
					new SignatureValueDTO(signatureValue.getAlgorithm(), signatureValue.getValue()));
			RemoteDocument signedDocument = soapMultiDocsClient.signDocument(signDocument);

			assertNotNull(signedDocument);

			DataToValidateDTO toValidate = new DataToValidateDTO(signedDocument, toSignDocuments, null);

			WSReportsDTO result = soapValidationService.validateSignature(toValidate);

			assertNotNull(result.getDiagnosticData());
			assertNotNull(result.getDetailedReport());
			assertNotNull(result.getSimpleReport());
			assertNotNull(result.getValidationReport());

			assertEquals(1, result.getSimpleReport().getSignatureOrTimestamp().size());
			
			XmlSignature signature = result.getDiagnosticData().getSignatures().get(0);
			assertEquals(SignatureLevel.JAdES_BASELINE_B, signature.getSignatureFormat());
			assertEquals(result.getSimpleReport().getSignatureOrTimestamp().get(0).getIndication(), Indication.INDETERMINATE);
			
			List<XmlDigestMatcher> digestMatchers = signature.getDigestMatchers();
			assertEquals(3, digestMatchers.size());
			
			for (XmlDigestMatcher digestMatcher : digestMatchers) {
				assertTrue(digestMatcher.isDataFound());
				assertTrue(digestMatcher.isDataIntact());
			}
		}
	}
	
	@Test
	public void testWithSignatureFieldId() throws Exception {
		try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(new FileInputStream("src/test/resources/user_a_rsa.p12"),
				new PasswordProtection("password".toCharArray()))) {

			List<DSSPrivateKeyEntry> keys = token.getKeys();
			DSSPrivateKeyEntry dssPrivateKeyEntry = keys.get(0);

			RemoteSignatureParameters parameters = new RemoteSignatureParameters();
			parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
			parameters.setSigningCertificate(new RemoteCertificate(dssPrivateKeyEntry.getCertificate().getCertificate().getEncoded()));
			parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
			parameters.setSignatureFieldId("signature-test");
	
			FileDocument fileToSign = new FileDocument(new File("src/test/resources/sample-with-empty-signature-fields.pdf"));
			RemoteDocument toSignDocument = new RemoteDocument(Utils.toByteArray(fileToSign.openStream()), fileToSign.getName());
	
			ToBeSignedDTO dataToSign = soapClient.getDataToSign(new DataToSignOneDocumentDTO(toSignDocument, parameters));
			assertNotNull(dataToSign);
			SignatureValue signatureValue = token.sign(DTOConverter.toToBeSigned(dataToSign), DigestAlgorithm.SHA256, dssPrivateKeyEntry);
			SignOneDocumentDTO signDocument = new SignOneDocumentDTO(toSignDocument, parameters, 
					new SignatureValueDTO(signatureValue.getAlgorithm(), signatureValue.getValue()));
			RemoteDocument signedDocument = soapClient.signDocument(signDocument);
			assertNotNull(signedDocument);
			
			DataToValidateDTO dataToValidateDTO = new DataToValidateDTO();
			dataToValidateDTO.setSignedDocument(signedDocument);
			WSReportsDTO wsReports = soapValidationService.validateSignature(dataToValidateDTO);
			
			DiagnosticData diagnosticData = new DiagnosticData(wsReports.getDiagnosticData());
			
			SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
			assertEquals(1, signature.getSignatureFieldNames().size());
			assertEquals("signature-test", signature.getSignatureFieldNames().get(0));
		}
	}

}
