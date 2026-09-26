package pcstore;
import com.vp.plugin.*;
import com.vp.plugin.diagram.*;
import com.vp.plugin.model.*;
import com.vp.plugin.model.factory.IModelElementFactory;
import java.io.File;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
public class ReportPlugin implements VPPlugin, VPPluginCommandLineSupport {
 public void loaded(VPPluginInfo info) {}
 public void unloaded() {}
 public void invoke(String[] args) {
  try {
   String output=args[0];
   if(args.length>1 && args[1].equals("verify")) {
    IProject project=ApplicationManager.instance().getProjectManager().getProject();
    System.out.println("REPORT01_REOPEN_OK diagrams="+project.toDiagramArray().length);
    System.exit(0);return;
   }
   ProjectManager pm=ApplicationManager.instance().getProjectManager();
   if(!pm.newProject()) throw new RuntimeException("newProject returned false");
   pm.getProject().setName("PC Store Report 01");
   DiagramManager dm=ApplicationManager.instance().getDiagramManager();
   DocumentBuilderFactory df=DocumentBuilderFactory.newInstance();
   df.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
   Document input=df.newDocumentBuilder().parse(new File(args[1]));
   NodeList diagrams=input.getElementsByTagName("Diagram");
   IModelElementFactory f=IModelElementFactory.instance();
   Map<String,IDBTable> tables=new HashMap<>();
   Map<String,IDBColumn> columns=new HashMap<>();
   Map<String,IDBForeignKey> foreignKeys=new HashMap<>();
   for(int k=0;k<diagrams.getLength();k++) {
    Element source=(Element)diagrams.item(k); boolean activity=source.getAttribute("kind").equals("Activity");
    boolean erd=source.getAttribute("kind").equals("ERD");
    // CE 18 Open API rejects DBForeignKey->DBTable in this installation.
    // ERD delivery remains the validated PlantUML source + PNG + dictionary.
    if(erd) continue;
    IDiagramUIModel d=dm.createDiagram(erd?IDiagramTypeConstants.DIAGRAM_TYPE_ER_DIAGRAM:(activity?DiagramManager.DIAGRAM_TYPE_ACTIVITY_DIAGRAM:DiagramManager.DIAGRAM_TYPE_USE_CASE_DIAGRAM));
    d.setName(source.getAttribute("id"));
    Map<String,IModelElement> models=new HashMap<>();
    Map<String,IDiagramElement> shapes=new HashMap<>();
    NodeList nodes=source.getElementsByTagName("Node");
    for(int j=0;j<nodes.getLength();j++) {
     Element n=(Element)nodes.item(j); String kind=n.getAttribute("kind"); IModelElement m;
     switch(kind) {
      case "Table":
       String tableName=n.getAttribute("id");
       IDBTable table=tables.get(tableName);
       if(table==null) {
        table=f.createDBTable();table.setDataModel(IDBTable.DATA_MODEL_PHYSICAL);table.setName(tableName);tables.put(tableName,table);
        NodeList cols=n.getElementsByTagName("Column");
        for(int z=0;z<cols.getLength();z++) {
         Element col=(Element)cols.item(z); IDBColumn c=table.createDBColumn();
         c.setName(col.getAttribute("name"));c.setTypeName(col.getAttribute("type"));
         c.setNullable(Boolean.parseBoolean(col.getAttribute("nullable")));c.setPrimaryKey(Boolean.parseBoolean(col.getAttribute("pk")));
         columns.put(tableName+"."+col.getAttribute("name"),c);
        }
       }
       m=table;break;
      case "Actor":m=f.createActor();break;
      case "UseCase":m=f.createUseCase();break;
      case "System":m=f.createSystem();break;
      case "Initial":m=f.createInitialNode();break;
      case "Final":m=f.createActivityFinalNode();break;
      case "Decision":m=f.createDecisionNode();break;
      case "Merge":m=f.createMergeNode();break;
      default:m=f.createActivityAction();break;
     }
     m.setName(n.getAttribute("name"));
     IShapeUIModel shape=(IShapeUIModel)dm.createDiagramElement(d,m);
     shape.setBounds(Integer.parseInt(n.getAttribute("x")),Integer.parseInt(n.getAttribute("y")),Integer.parseInt(n.getAttribute("w")),Integer.parseInt(n.getAttribute("h")));
     shape.setRequestResetCaption(true);
     models.put(n.getAttribute("id"),m);shapes.put(n.getAttribute("id"),shape);
    }
    if(!activity && !erd) {
     for(String id:shapes.keySet()) if(id.startsWith("UC")) ((IShapeUIModel)shapes.get("Boundary")).addChild((IShapeUIModel)shapes.get(id));
     ((IActor)models.get("Visitor")).setAbstract(true);
    }
    NodeList edges=source.getElementsByTagName("Edge");
    for(int j=0;j<edges.getLength();j++) {
     Element e=(Element)edges.item(j);String a=e.getAttribute("source"),b=e.getAttribute("target");
     IRelationship r;
     if(erd) {
      String fkKey=b+"."+e.getAttribute("label");IDBForeignKey fk=foreignKeys.get(fkKey);
      if(fk==null) {
       fk=f.createDBForeignKey();fk.setDataModel(IDBForeignKey.DATA_MODEL_PHYSICAL);fk.setName("fk_"+b+"_"+e.getAttribute("label"));
       fk.setFrom(models.get(a));fk.setTo(models.get(b));
       fk.setFromMultiplicity(Boolean.parseBoolean(e.getAttribute("nullable"))?IDBForeignKey.FROM_MULTIPLICITY_ZERO_TO_ONE:IDBForeignKey.FROM_MULTIPLICITY_ONE);
       fk.setToMultiplicity(Boolean.parseBoolean(e.getAttribute("single"))?IDBForeignKey.TO_MULTIPLICITY_ZERO_TO_ONE:IDBForeignKey.TO_MULTIPLICITY_ZERO_TO_MANY);
       fk.setOnDelete(IDBForeignKey.ON_DELETE_RESTRICT);
       IDBForeignKeyConstraint constraint=f.createDBForeignKeyConstraint();constraint.setForeignKey(fk);constraint.setRefColumn(columns.get(a+".id"));
       columns.get(fkKey).addForeignKeyConstraint(constraint);foreignKeys.put(fkKey,fk);
      }
      r=fk;
     }
     else if(activity) {IControlFlow flow=f.createControlFlow();flow.setGuard(e.getAttribute("label"));r=flow;}
     else if(e.getAttribute("kind").equals("Generalization")) r=f.createGeneralization();
     else r=f.createAssociation();
     r.setFrom(models.get(a));r.setTo(models.get(b));
     dm.createConnector(d,r,shapes.get(a),shapes.get(b),null);
    }
    System.out.println("CREATED "+d.getName());
   }
   if(!pm.saveProjectAs(new File(output))) throw new RuntimeException("saveProjectAs returned false");
   // Export XML separately with com.vp.cmd.ExportXML; the Open API export opens a Swing dialog.
   System.out.println("REPORT01_OK "+output);
   System.exit(0);
  } catch(Throwable ex) {ex.printStackTrace();System.out.println("REPORT01_FAILED");}
 }
}
