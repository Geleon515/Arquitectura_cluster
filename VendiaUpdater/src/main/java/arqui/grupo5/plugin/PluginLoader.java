package arqui.grupo5.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public class PluginLoader {

    private final List<VendiaPlugin> plugins = new ArrayList<>();
    private final Consumer<String>   logger;

    public PluginLoader(Consumer<String> logger) {
        this.logger = logger;
    }

    public void cargarDesde(String rutaCarpeta) {
        File carpeta = new File(rutaCarpeta);
        if (!carpeta.isDirectory()) {
            logger.accept("PLUGINS: carpeta '" + rutaCarpeta + "' no encontrada, sin plugins activos.");
            return;
        }
        File[] jars = carpeta.listFiles(f -> f.getName().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            logger.accept("PLUGINS: ningun JAR encontrado en " + rutaCarpeta);
            return;
        }
        for (File jar : jars) {
            try {
                URL[] urls = { jar.toURI().toURL() };
                ClassLoader parent = Thread.currentThread().getContextClassLoader();
                URLClassLoader loader = new URLClassLoader(urls, parent);
                ServiceLoader<VendiaPlugin> sl = ServiceLoader.load(VendiaPlugin.class, loader);
                int antes = plugins.size();
                for (VendiaPlugin p : sl) {
                    plugins.add(p);
                    logger.accept("PLUGIN cargado: [" + p.getNombre() + "] desde " + jar.getName());
                }
                if (plugins.size() == antes) {
                    logger.accept("PLUGIN AVISO: " + jar.getName() + " no registra VendiaPlugin.");
                }
            } catch (Exception e) {
                logger.accept("PLUGIN ERROR cargando " + jar.getName() + ": " + e.getMessage());
            }
        }
        logger.accept("PLUGINS activos: " + plugins.size());
    }

    public void ejecutarTodos(String idVenta, String idVendedor,
                               String idProducto, String fecha, double monto) {
        for (VendiaPlugin p : plugins) {
            try {
                p.ejecutar(idVenta, idVendedor, idProducto, fecha, monto);
            } catch (Exception e) {
                logger.accept("PLUGIN ERROR en [" + p.getNombre() + "]: " + e.getMessage());
            }
        }
    }

    public int totalPlugins() { return plugins.size(); }
}
