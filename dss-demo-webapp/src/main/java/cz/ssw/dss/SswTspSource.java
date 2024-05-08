package cz.ssw.dss;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.MimeType;
import eu.europa.esig.dss.model.*;
import eu.europa.esig.dss.service.NonceSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.HostConnection;
import eu.europa.esig.dss.service.http.commons.TimestampDataLoader;
import eu.europa.esig.dss.service.http.commons.UserCredentials;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.spi.DSSASN1Utils;
import eu.europa.esig.dss.spi.client.http.DataLoader;
import eu.europa.esig.dss.spi.exception.DSSExternalResourceException;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import eu.europa.esig.dss.utils.Utils;
import org.apache.cxf.configuration.security.KeyStoreType;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;


    public class SswTspSource extends OnlineTSPSource implements TSPSource {
        private static final Logger LOG = LoggerFactory.getLogger(SswTspSource.class);

        /**
         * Builds an SswTspSource that will query the URL and the specified {@code DataLoader}
         *
         * @param tspServer  the tsp URL - WARNING : MUST INCLUDE TRAILING "/" !!!!
         * @param username username used by basic authorization
         * @param  password password  used by basic authorization
         */
        public SswTspSource(final String tspServer, final String username, final String password) {
            this.setTspServer(tspServer);
            URI uri= URI.create(tspServer);
            TimestampDataLoader dl =  new TimestampDataLoader();
            dl.addAuthentication(uri.getHost(),uri.getPort(),uri.getScheme(), username,password.toCharArray());
            dl.setPreemptiveAuthentication(true);
            this.setDataLoader(dl);
            LOG.trace("+SswTspSource configured with username and password.");
        }


        public SswTspSource(final String tspServer,
                            final String SslKeystoreType,
                            final String SslKeystoreResource,
                            final String SslKeystorePassword
        ) {
            new SswTspSource(tspServer, SslKeystoreType, SslKeystoreResource, SslKeystorePassword, false);
        }

        public SswTspSource(final String tspServer,
                            final String sslKeystoreType,
                            final String sslKeystoreResource,
                            final String sslKeystorePassword,
                            boolean useKeyAsTrustMaterial
        ) {
            this.setTspServer(tspServer);
            URI uri= URI.create(tspServer);
            TimestampDataLoader dl =  new TimestampDataLoader();
            this.setDataLoader(dl);
            dl.setSslKeystoreType(sslKeystoreType);//""PKCS12");
            dl.setSslKeystorePassword(sslKeystorePassword.toCharArray());
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream KeyStoreStream = classloader.getResourceAsStream( sslKeystoreResource);// "postsignum_keys.p12");
            dl.setSslKeystore(new InMemoryDocument(KeyStoreStream));
            dl.setKeyStoreAsTrustMaterial(useKeyAsTrustMaterial);
            this.setDataLoader(dl);
            LOG.trace("+SswTspSource configured with keystore.");
        }




        /**
         * Builds an SswTspSource that will query the URL and the specified {@code DataLoader}
         *
         * @param tspServer  the tsp URL
         * @param dataLoader {@link DataLoader} to retrieve the TSP
         */
        public SswTspSource(final String tspServer, final DataLoader dataLoader) {
            super(tspServer,dataLoader);
            LOG.trace("+SswTspSource with the specific data loader.");
        }

    }
