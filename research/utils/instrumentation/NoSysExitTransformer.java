package research.utils.instrumentation;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtBehavior;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import java.net.MalformedURLException;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.Mnemonic;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Descriptor;
import javassist.bytecode.Bytecode;

public class NoSysExitTransformer implements ClassFileTransformer {

	ClassLoader class_loader = null;		
	public static boolean sysexit_done=false;
	public static Object sysexit_lock=null;	
	public NoSysExitTransformer() {
		try {
			File dir = new File(System.getProperty("java.class.path"));
			URL url = dir.toURL();       // file:/c:/almanac1.4/examples/
			URL[] urls = new URL[]{url};
			class_loader = new URLClassLoader(urls);
		} catch (MalformedURLException e) {
			e.printStackTrace();		
		}
	}

	public byte[] transform(ClassLoader loader, String className,
			Class classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		
			
		byte[] byteCode = classfileBuffer;
		if(className == null) return byteCode;
		if(!className.equals("java/lang/System")) return byteCode;

		try {
			ClassPool classPool = ClassPool.getDefault();
		    	CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
			System.out.println("Instrumenting::" + ctClass.getName());
			if(sysexit_done) return byteCode;
			sysexit_done=true;
			CtMethod ctMethod = ctClass.getMethod("exit", Descriptor.ofMethod(CtClass.voidType, new CtClass[] {CtClass.intType}));
			ctMethod.insertBefore("if(true) return;");
			byteCode = ctClass.toBytecode();
		        ctClass.detach();
			return byteCode;
		} catch (Throwable ex) {
			ex.printStackTrace();
			//System.out.println("Any Exception at this point ???");
		}

		return byteCode;
	}


	/*public static String void_non() {
		 String desc = Descriptor.ofMethod(CtClass.voidType, null);
		 return desc;
	}*/

}
