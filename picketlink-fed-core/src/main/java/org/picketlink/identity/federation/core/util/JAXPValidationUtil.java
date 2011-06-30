/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.picketlink.identity.federation.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Utility class associated with JAXP Validation
 * @author Anil.Saldhana@redhat.com
 * @since Jun 30, 2011
 */
public class JAXPValidationUtil
{
   protected static Logger log = Logger.getLogger(JAXPValidationUtil.class);

   protected static boolean trace = log.isTraceEnabled();

   protected static Validator validator;

   protected static SchemaFactory schemaFactory;

   public static void validate(String str) throws SAXException, IOException
   {
      validator().validate(new StreamSource(str));
   }

   public static void validate(InputStream stream) throws SAXException, IOException
   {
      validator().validate(new StreamSource(stream));
   }

   public static Validator validator() throws SAXException, IOException
   {
      String schemaFactoryProperty = "javax.xml.validation.SchemaFactory:" + XMLConstants.W3C_XML_SCHEMA_NS_URI;
      SecurityActions.setSystemProperty(schemaFactoryProperty, "org.apache.xerces.jaxp.validation.XMLSchemaFactory");

      if (validator == null)
      {
         Schema schema = getSchema();
         if (schema == null)
            throw new RuntimeException("Could not get all the schemas");

         validator = schema.newValidator();
         validator.setErrorHandler(new CustomErrorHandler());
      }
      return validator;
   }

   private static Schema getSchema() throws IOException
   {
      schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

      schemaFactory.setResourceResolver(new IDFedLSInputResolver());
      schemaFactory.setErrorHandler(new CustomErrorHandler());
      Schema schemaGrammar = null;
      try
      {
         schemaGrammar = schemaFactory.newSchema(sources());
      }
      catch (SAXException e)
      {
         log.error("Cannot get schema", e);
      }
      return schemaGrammar;
   }

   private static Source[] sources() throws IOException
   {
      List<String> schemas = SchemaManagerUtil.getSchemas();

      Source[] sourceArr = new Source[schemas.size()];

      int i = 0;
      for (String schema : schemas)
      {
         URL url = SecurityActions.loadResource(JAXPValidationUtil.class, schema);
         if (url == null)
            throw new RuntimeException(schema + " is not available");
         sourceArr[i++] = new StreamSource(url.openStream());
      }
      return sourceArr;
   }

   private static class CustomErrorHandler implements ErrorHandler
   {
      public void error(SAXParseException ex) throws SAXException
      {
         logException(ex);
         if (ex.getMessage().contains("null") == false)
         {
            throw ex;
         }
      }

      public void fatalError(SAXParseException ex) throws SAXException
      {
         logException(ex);
         throw ex;
      }

      public void warning(SAXParseException ex) throws SAXException
      {
         logException(ex);
      }

      private void logException(SAXParseException sax)
      {
         StringBuilder builder = new StringBuilder();

         if (trace)
         {
            builder.append("[").append(sax.getLineNumber()).append(",").append(sax.getColumnNumber()).append("]");
            builder.append(":").append(sax.getLocalizedMessage());
            log.trace(builder.toString());
         }
      }
   };
}