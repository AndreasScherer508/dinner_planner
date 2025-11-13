open module edu.sb.dinner_planner.model {
	// declares dependencies
	requires transitive java.sql;
	requires transitive java.logging;
	requires transitive jakarta.annotation;
	requires transitive jakarta.validation;
	requires transitive jakarta.json.bind;
	requires transitive jakarta.xml.bind;
	requires transitive jakarta.persistence;
	requires transitive eclipselink;
	requires jakarta.ws.rs;

	// grants accessibility of the given packages to dependent projects
	exports edu.sb.tool;
	exports edu.sb.dinner_planner.persistence;
	exports edu.sb.dinner_planner.service;
}