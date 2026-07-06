package arqui.grupo5.web.soap;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    /**
     * Registra el MessageDispatcherServlet en la ruta /ws/*
     * para que todas las peticiones SOAP pasen por Spring-WS.
     */
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {

        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    /**
     * Define el WSDL que se genera automáticamente a partir del XSD.
     *
     * El nombre del bean ("ventas") determina la URL del WSDL:
     *   → http://localhost:8080/ws/ventas.wsdl
     */
    @Bean(name = "ventas")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema ventasSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("VentasPort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace("http://arqui.grupo5.web/soap/ventas");
        wsdl.setSchema(ventasSchema);
        return wsdl;
    }

    /**
     * Carga el esquema XSD desde los resources del classpath.
     */
    @Bean
    public XsdSchema ventasSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/ventas.xsd"));
    }
}
