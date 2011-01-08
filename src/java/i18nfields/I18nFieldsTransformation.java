package i18nfields;

import groovy.util.ConfigSlurper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.springframework.asm.Opcodes;

/**
  * TODO: New fields might be nullable if the language-aware getter will fallback to the default
  * if no content is provided.
  *	 DONE: Nullable constraint is taken into account, and can be overriden for _xx fields
  * TODO: Default lang field *must* have a value... do we need the field or will we assume that th
  * original field ("name") is the backend for the default language?
  * TODO: Should the default lang be specified and thus no field created for it?
  * TODO: If the getter to be generated already exists (some process wrapping the field), we will WARN
  * and leave it unchanged. It's then programmer's responsability to provide access to i18n fields
  * TODO: Provide a wrapper to LCH so it's easy to program ad-hoc i18n inside the classes (protected method?)
  *	 DONE: I18nFieldsHelper.groovy
  * TODO: Provide a setter for language (LCH??) so we can inject a custom i18n to an object if needed
  *	 DONE: I18nFieldsHelper.groovy (sets into the thread)
  */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class I18nFieldsTransformation implements ASTTransformation {

	private static final Log LOG = LogFactory.getLog(I18nFieldsTransformation.class);
	private static final Properties CONF = (new ConfigSlurper().parse(getContents(new File("./grails-app/conf/Config.groovy")))).toProperties();


	public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {

		for (ASTNode astNode : astNodes) {
			if (astNode instanceof ClassNode) {
				ClassNode classNode = (ClassNode) astNode;
				ListExpression fields = (ListExpression)classNode.getField("i18n_fields").getInitialValueExpression();
				//ListExpression langs = (ListExpression)classNode.getField("i18n_langs").getInitialValueExpression();
				String l = CONF.getProperty("i18nFields.i18n_langs", "");
				String[] langs = l.substring(1,l.length()-1).split(",");
				
				if (fields != null && langs != null) {
					System.out.println("Adding i18n fields to " + classNode.getName());
					for (Iterator<Expression> itt = fields.getExpressions().iterator(); itt.hasNext(); ) {
						String field = (String)((ConstantExpression)itt.next()).getValue();
						if (classNode.getField(field) == null) {
							System.out.println("* ERROR: There's no such field '" + field + "' as stated in i18n_fields declaration.");
						} else {
							System.out.println("* Field: " + field);
							makeTransientField(classNode, field);
							for (String it: langs) {
								String lang = it.trim();
								System.out.println("  - " + lang);
								classNode.addProperty(field + "_" + lang, Modifier.PUBLIC, new ClassNode(String.class), new ConstantExpression(lang), null, null);
								addNullableConstraint(classNode, field + "_" + lang, getConstraintsAgrument(classNode, field));
							}
						}
					}
				}
			}
		}
	}

	
	public boolean isNullable(ClassNode classNode, String field) {
		FieldNode constraints = classNode.getDeclaredField("constraints");
		if (constraints != null) {
			ClosureExpression exp = (ClosureExpression)constraints.getInitialExpression();
			BlockStatement block = (BlockStatement)exp.getCode();
			List<Statement> ments = block.getStatements();
			for(Statement expstat : ments) {
				if(expstat instanceof ExpressionStatement && ((ExpressionStatement)expstat).getExpression() instanceof MethodCallExpression){
					MethodCallExpression methexp = (MethodCallExpression)((ExpressionStatement)expstat).getExpression();
					if (methexp.getMethodAsString().equals(field)) {
						System.out.println(methexp.getArguments() instanceof MapExpression );
						System.out.println(methexp.getArguments() instanceof NamedArgumentListExpression );
						System.out.println(methexp.getArguments() instanceof Expression );
						System.out.println(methexp.getArguments() instanceof ConstantExpression );
						System.out.println("------------------------------");
						System.out.println(getArguments(methexp) );
						if(methexp.getArguments() instanceof org.codehaus.groovy.ast.expr.ArgumentListExpression){
							continue;
						}
						for(Expression expression : getArguments(methexp)){
							
							if(expression instanceof NamedArgumentListExpression ){
								
								NamedArgumentListExpression args = (org.codehaus.groovy.ast.expr.NamedArgumentListExpression)expression;
								for (MapEntryExpression it : args.getMapEntryExpressions() ) {
									MapEntryExpression mee = it;
									String nullable = (String)((ConstantExpression)mee.getKeyExpression()).getValue();
									if (nullable.equals("nullable"))
										return ((Boolean)((ConstantExpression)mee.getValueExpression()).getValue()).booleanValue();
								}
							}
						}
					}
				}
			}
		}				
		return false;
	}
	
	private NamedArgumentListExpression getConstraintsAgrument(ClassNode classNode, String field) {
		FieldNode constraints = classNode.getDeclaredField("constraints");
		if (constraints != null) {
			ClosureExpression exp = (ClosureExpression)constraints.getInitialExpression();
			BlockStatement block = (BlockStatement)exp.getCode();
			List<Statement> ments = block.getStatements();
			for(Statement expstat : ments) {
				if(expstat instanceof ExpressionStatement && ((ExpressionStatement)expstat).getExpression() instanceof MethodCallExpression){
					MethodCallExpression methexp = (MethodCallExpression)((ExpressionStatement)expstat).getExpression();
					if (methexp.getMethodAsString().equals(field)) {
						if(methexp.getArguments() instanceof org.codehaus.groovy.ast.expr.ArgumentListExpression){
							continue;
						}
						for(Expression expression : getArguments(methexp)){
							
							if(expression instanceof NamedArgumentListExpression ){
								NamedArgumentListExpression args = (org.codehaus.groovy.ast.expr.NamedArgumentListExpression)expression;
								return args;
							}
						}
					}
				}
			}
		}				
		return null;
	}
	

	
  
  private List<Expression> getArguments(MethodCallExpression expr) {
	// MethodCallExpression.getArguments() may return an ArgumentListExpression,
	// a TupleExpression that wraps a NamedArgumentListExpression,
	// or (at least in 1.6) a NamedArgumentListExpression

	if (expr.getArguments() instanceof NamedArgumentListExpression)
	  // return same result as for NamedArgumentListExpression wrapped in TupleExpression
	  return java.util.Collections.singletonList(expr.getArguments());
	
	return ((TupleExpression) expr.getArguments()).getExpressions();
   }
	
	public void addNullableConstraint(ClassNode classNode,String fieldName,  NamedArgumentListExpression namedArgs){
		FieldNode closure = classNode.getDeclaredField("constraints");
		if(closure!=null && !hasNullableConstraint(classNode, fieldName)){
			ClosureExpression exp = (ClosureExpression)closure.getInitialExpression();
			BlockStatement block = (BlockStatement) exp.getCode();
			if(namedArgs != null){
				MethodCallExpression constExpr = new MethodCallExpression(
					VariableExpression.THIS_EXPRESSION,
					new ConstantExpression(fieldName),
					namedArgs
					);
				block.addStatement(new ExpressionStatement(constExpr));
			}
			//System.out.println("Added " +fieldName + " 'nullable: " + value + "' constraint.");
		}
	}
	
	public void makeTransientField(ClassNode classNode, String fieldName) {
		FieldNode closure = classNode.getDeclaredField("transients");
		if (closure == null) {
			System.out.println("\n\nWARNING: '" + fieldName + "' has not been made transient because there's no 'static transients = []' declataion. Add one if you don't want this column to be created in the table.\n\n");
			closure = createTransientStaticBlock(classNode);
		}
		assert closure != null;
		List<Expression> transients = ((ListExpression)closure.getInitialValueExpression()).getExpressions();
		for (Iterator<Expression> it = transients.iterator(); it.hasNext(); ) {
			if (((ConstantExpression)it.next()).getValue().equals(fieldName))
				return;
		}
		((ListExpression)closure.getInitialValueExpression()).addExpression(new ConstantExpression(fieldName));
		System.out.println("  * " + fieldName + " has been made transient");
	}

	public FieldNode createTransientStaticBlock(ClassNode classNode) {
		FieldNode transients = classNode.getDeclaredField("transients");
		if (null == transients) {
			transients = new FieldNode("transients", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, new ClassNode(Object.class), classNode, new ListExpression());
			transients.setDeclaringClass(classNode);
			classNode.addField(transients);
		}
		return transients;
	}
	
	public boolean hasNullableConstraint(ClassNode classNode, String fieldName) {
		FieldNode closure = classNode.getField("constraints");
		if (closure != null) {
			ClosureExpression exp = (ClosureExpression)closure.getInitialExpression();
			BlockStatement block = (BlockStatement)exp.getCode();
			List<Statement> ments = block.getStatements();
			for(Statement expstat : ments) {
				if(expstat instanceof ExpressionStatement && ((ExpressionStatement)expstat).getExpression() instanceof MethodCallExpression){
					MethodCallExpression methexp = (MethodCallExpression)((ExpressionStatement)expstat).getExpression();
					if (methexp.getMethodAsString().equals(fieldName)) {
						System.out.println("methexp.getArguments();-->"+ methexp.getArguments());
						MapExpression args = (MapExpression)methexp.getArguments();
						for (Iterator<MapEntryExpression> it = args.getMapEntryExpressions().iterator(); it.hasNext(); ) {
							MapEntryExpression mee = it.next();
							String nullable = (String)((ConstantExpression)mee.getKeyExpression()).getValue();
							if (nullable.equals("nullable"))
								return true;
						}
					}
				}
			}
		}
		return false;
	}

	public void cloneNullableConstraint(ClassNode classNode, String fieldName) {
		FieldNode closure = classNode.getDeclaredField("constraints");
		if (closure != null) {
			ClosureExpression exp = (ClosureExpression)closure.getInitialExpression();
			BlockStatement block = (BlockStatement)exp.getCode();
			List<Statement> ments = block.getStatements();
			for(Statement expstat : ments){
				if(expstat instanceof ExpressionStatement && ((ExpressionStatement)expstat).getExpression() instanceof MethodCallExpression){
					MethodCallExpression methexp = (MethodCallExpression)((ExpressionStatement)expstat).getExpression();
					MapExpression args = (MapExpression)methexp.getArguments();
					for (Iterator<MapEntryExpression> it = args.getMapEntryExpressions().iterator(); it.hasNext(); ) {
						MapEntryExpression mee = it.next();
						if (((String)((ConstantExpression)mee.getKeyExpression()).getValue()).equals("nullable")) {
							boolean b = ((Boolean)((ConstantExpression)mee.getValueExpression()).getValue()).booleanValue();
							NamedArgumentListExpression namedarg = new NamedArgumentListExpression();
							namedarg.addMapEntryExpression(new ConstantExpression("nullable"), new ConstantExpression(b));
							MethodCallExpression constExpr = new MethodCallExpression(
								VariableExpression.THIS_EXPRESSION,
								new ConstantExpression(fieldName),
								namedarg
								);
							block.addStatement(new ExpressionStatement(constExpr));
						}
					}
				}
			}
		}
	}

	/** 
	This method helps to read the config.groovy
	*/
	static private String getContents(File aFile) {
		//...checks on aFile are elided
		StringBuilder contents = new StringBuilder();

		try {
			//use buffering, reading one line at a time
			//FileReader always assumes default encoding is OK!
			BufferedReader input =  new BufferedReader(new FileReader(aFile));
			try {
				String line = null; 
				while (( line = input.readLine()) != null){
					contents.append(line);
					contents.append(System.getProperty("line.separator"));
				}
			}
			finally {
				input.close();
			}
		}
		catch (IOException ex){
			ex.printStackTrace();
		}
		return contents.toString();
	}
}

