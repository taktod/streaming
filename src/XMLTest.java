import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;


public class XMLTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// encode.xmlの読み込みテスト
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Node root = builder.parse(new File("encode.xml"));
		check(root);
	}
	private static void check(Node node) {
		System.out.print(node.getNodeName());
		System.out.print(":");
		System.out.println(node.getNodeValue());
		for(int i = 0;i < node.getChildNodes().getLength();i ++) {
			check(node.getChildNodes().item(i));
		}
	}

}
