package gart.demo

class GartDemo {

//	enum Category { ALFA, BETA }
	
//	Category category 
	String name
	String email
//	String lista
//	String pass
//	int orden
//	BigDecimal bd
//	Date fecha
	
    static constraints = {
		email  email: true
//		orden blank: false, min: 18, max: 99
//		pass password: true, attributes: [yo: 'JAC', otro: 'pirulo'], matches: "[a-zA-Z]+"
//		lista inList: ["Joe", "Fred", "Bob"]
//		bd range: (100)..(59999.99), scale:2
    }
}
