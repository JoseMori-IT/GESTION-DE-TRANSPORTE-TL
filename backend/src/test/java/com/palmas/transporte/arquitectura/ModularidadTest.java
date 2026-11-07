package com.palmas.transporte.arquitectura;


import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;

/** Límites de fuentes verificados en cada ejecución Maven; no requiere bibliotecas externas. */
class ModularidadTest {
 private static final Path RAIZ=Path.of("src/main/java");
 private static final Pattern PAQUETE=Pattern.compile("package\\s+([\\w.]+)\\s*;");
 private static final Pattern IMPORT=Pattern.compile("import\\s+(com\\.palmas\\.transporte[\\w.]+)\\s*;");
 private static final Pattern MODULO=Pattern.compile("\\.modulos\\.([a-z]+)\\.");
 private String modulo(String nombre){var m=MODULO.matcher(nombre);return m.find()?m.group(1):"plataforma";}
 @Test void paquetesCorrespondenALasCarpetasYNoRegresanACapasGlobales()throws Exception{
  try(var archivos=Files.walk(RAIZ)){
   for(var p:archivos.filter(x->x.toString().endsWith(".java")).toList()){
    var texto=Files.readString(p).replace("\\ufeff", "");var m=PAQUETE.matcher(texto);assertTrue(m.find(),p.toString());
    assertEquals(RAIZ.resolve(m.group(1).replace('.','/')).normalize(),p.getParent().normalize(),p.toString());
    assertFalse(m.group(1).matches("com\\.palmas\\.transporte\\.(controller|service|model|repository|dto|config)"),p.toString());
    assertFalse(texto.matches("(?s).*import\\s+com\\.palmas\\.transporte[^;]*\\.\\*\\s*;.*"),"Import interno ambiguo: "+p);
   }
  }
 }
 @Test void contratosPublicosNoDependenDePersistenciaAplicacionNiWeb()throws Exception{
  try(var archivos=Files.walk(RAIZ)){
   for(var p:archivos.filter(x->x.toString().endsWith(".java")&&x.toString().contains("api")).toList()){
    var m=IMPORT.matcher(Files.readString(p));while(m.find())assertFalse(m.group(1).matches(".*\\.(persistence|application|web)\\..*"),p+" -> "+m.group(1));
   }
  }
 }
 @Test void interfacesWebNoSeAcoplanAControladoresDeOtroModulo()throws Exception{
  try(var archivos=Files.walk(RAIZ)){
   for(var p:archivos.filter(x->x.toString().endsWith(".java")).toList()){
    var t=Files.readString(p);var pkg=PAQUETE.matcher(t);assertTrue(pkg.find());var origen=modulo(pkg.group(1));if(origen.equals("plataforma"))continue;
    var m=IMPORT.matcher(t);while(m.find())if(m.group(1).contains(".web."))assertEquals(origen,modulo(m.group(1)),p+" -> "+m.group(1));
   }
  }
 }
 @Test void accesosHeredadosARepositoriosAjenosNoAumentan()throws Exception{
  Set<String> actual=new TreeSet<>();
  try(var archivos=Files.walk(RAIZ)){
   for(var p:archivos.filter(x->x.toString().endsWith(".java")).toList()){
    var t=Files.readString(p);var pkg=PAQUETE.matcher(t);assertTrue(pkg.find());var origen=modulo(pkg.group(1));if(origen.equals("plataforma"))continue;
    var m=IMPORT.matcher(t);while(m.find())if(m.group(1).contains(".persistence.")&&!origen.equals(modulo(m.group(1))))actual.add(pkg.group(1)+"."+p.getFileName().toString().replace(".java","")+" -> "+m.group(1));
   }
  }
  var permitidas=new TreeSet<>(Files.readAllLines(Path.of("src/test/resources/arquitectura/persistencia-heredada.txt")));permitidas.removeIf(String::isBlank);
  assertEquals(permitidas,actual,"Reducir dependencias heredadas; cualquier nueva excepción requiere revisión de arquitectura.");
  assertTrue(actual.stream().noneMatch(x->x.startsWith("com.palmas.transporte.modulos.abastecimientos.")),"Abastecimientos solo accede a Flota por su contrato API.");
 }
}
