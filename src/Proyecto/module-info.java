// module-info.java
// Asegúrate que esté en la raíz de tu código fuente del módulo,
// por ejemplo: TareaProject/src/Tarea/module-info.java

module Proyecto { // Este es el nombre de TU módulo

	exports common;
	requires java.rmi;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.annotation;
	requires org.apache.httpcomponents.client5.httpclient5;
	requires org.apache.httpcomponents.core5.httpcore5;
	requires java.sql;


}
