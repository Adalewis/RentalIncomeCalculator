import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javafx.application.*;
import javafx.collections.*;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.*;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RentalIncome extends Application {
	public void start(Stage primaryStage) {
		ListView<String> listView = new ListView<>();
		List<String> list = webScrapeValues();
		ObservableList<String> items = FXCollections.observableArrayList(list);
		listView.setItems(items);
		Label label = new Label();
		label.setTextFill(Color.BLUE);
		
		listView.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends String> ov, String old_val, String new_val) -> {
		String selectedItem = listView.getSelectionModel().getSelectedItem();     
		int priceInd = selectedItem.indexOf("$", 0);
		String price = selectedItem.substring(priceInd+1);
		int num = Integer.parseInt(price.replaceAll(",", ""));
		
		String results = calculate(num);
		label.setText(results);
		});
		HBox hBox = new HBox(30, listView, label);
		listView.setMaxSize(400, 500);
		listView.setPrefSize(400, 500);
		Scene scene = new Scene(hBox, 1200, 600);
		  /* Set the scene to primaryStage, and call the show method */
		primaryStage.setTitle("Rental Income Calculator");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	public static ArrayList<String> webScrapeValues() {
		Document doc = null;
		ArrayList<String> values = new ArrayList<String>();
		try {
			doc = Jsoup.connect("http://www.brevardbeachhomes.com/melbourne-real-estate-for-sale.php?sortorder=ASC-ListingDOM").get();
			Elements info = doc.select("article h3");
			Elements desc = doc.select("article div.property-type");
			
			for (Element tr: info) {
				String property = tr.text();
				int priceInd = property.indexOf("$", 0);
				values.add(property);
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return values;
	}
	
	
	public static double initialCosts(double payment, int price) {
		double closingCost = 3000.00;
		double downPayment = payment;
		double purchasePrice = price;
		double initialCostSum = closingCost + ((downPayment*purchasePrice)*.01);
		return initialCostSum;
	}
	public static double recurringExpenses(int year) {
		double annualInc = .03*year;
		double annualTax = 1200.00;
		double insur = 1200.00;
		double maint = 1000.00;
		double unexpected = 200.00;
		double annualCosts = annualTax + insur + maint + unexpected;
		annualCosts += (annualCosts*annualInc);
		return annualCosts;
	}
	public static double[] rentalInc(int price, int year) {
		double purchasePrice = price;
		double annualInc = .03*year;
		double rentHigh = .011;
		double rentLow = .008;
		double yearlyRentHigh = (purchasePrice*rentHigh)*12;
		yearlyRentHigh += (yearlyRentHigh*annualInc);
		double yearlyRentLow = (purchasePrice*rentLow)*12;
		yearlyRentLow += (yearlyRentLow*annualInc);
		double[] rent = new double[2];
		rent[0] = yearlyRentLow;
		rent[1] = yearlyRentHigh;
		return rent;
	}
	public static double mortgage(double payment, int price, int loanLen) {
		
		int monthsToPay = 0;
		double intRate = 0;
		double purchasePrice = price;
		double downPayment = purchasePrice * payment;
		int loanTerm = loanLen;
		if (loanTerm == 10) {
			intRate = .0246;
			monthsToPay = 120;
		} else if (loanTerm == 15) {
			intRate = .0248;
			monthsToPay = 180;
		} else {
			intRate = .025;
			monthsToPay = 240;
		}
		double interest = (purchasePrice-downPayment)*intRate;
		double cost = (purchasePrice-downPayment) + interest;
		double mortgage = cost/monthsToPay;
		double yearlyMortgage = mortgage*12;
		return mortgage;
	}
	public static double yearlyCosts(int year, int price, int loanLen) {
		
		double payment = .20;
		double mortgage = mortgage(payment, price, loanLen);
		double expenses = recurringExpenses(year);
		double totalExpYearly = mortgage+expenses;
		
		return totalExpYearly;
		 
	}
	
	public static String calculate(int num) {
		int price = num;
		int loanLen = 20;
		double payment = .20;
		int year = 1;
		double initialCosts = initialCosts(payment, price);
		double firstYearCost = yearlyCosts(1, price, loanLen);
		double[] firstYearInc = rentalInc(price, year);
		
		int pessimistic = (int) Math.rint(firstYearInc[0] - (initialCosts+firstYearCost));
		int optimistic = (int) Math.rint(firstYearInc[1] - (initialCosts+firstYearCost));
		String a = "Calculated income for first year would be between " + "$"+pessimistic +"-"+ "$"+optimistic + "\n";
		String b = "";
		for (int i = 1; i < loanLen; i++) {
			year += 1;
			double yearlyCost = yearlyCosts(year, price, loanLen);
			double[] passiveInc = rentalInc(price, year);
			optimistic = (int) Math.rint(passiveInc[1] - (yearlyCost));	
			pessimistic = (int) Math.rint(passiveInc[0] - (yearlyCost));
			b += "Calculated income for year " + year + " would be between " + "$"+  pessimistic +"-"+ "$"+ optimistic + "\n";
			
		}
		year +=1;
		double[] passiveInc = rentalInc(price, year);
		optimistic = (int) Math.rint(passiveInc[1]);
		pessimistic = (int) Math.rint(passiveInc[0]);
		String c = "Calculated income for year " + year + " would be between " + "$"+pessimistic +"-"+ "$"+optimistic;
		String result = a.concat(b).concat(c);
		BufferedWriter writer = null;
		try {
			FileWriter file = new FileWriter("test.txt");
			file.write(result);
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Application.launch(args);
		
		
		
	}
	
}

