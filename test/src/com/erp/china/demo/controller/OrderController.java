package com.erp.china.demo.controller;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.erp.china.demo.model.Order;
import com.erp.china.demo.model.Customer;
import com.erp.china.demo.service.BookingService;
import com.erp.china.demo.service.OrderService;
import com.erp.china.demo.service.CustomerService;
import com.erp.china.demo.util.Constants;

@Controller
@RequestMapping("/OD")
public class OrderController {
	private static OrderService orderService;
	private static BookingService bookingService;
	private Logger logger;
	private static Map<String, String> orderMap;

	public OrderController() {
		logger = Logger.getLogger(OrderController.class);
		orderMap = new ConcurrentHashMap();
		if (orderService == null) {
			orderService = OrderService.getInstance();
		}
		if (bookingService == null) {
			bookingService = BookingService.getInstance();
		}
		initOrderMap();
	}

	public static void initOrderMap() {
		List<Order> orderList = orderService.getOrderList();
		for (Order order : orderList) {
			orderMap.put(order.getOrderId(), order.getOrderNumber());
		}
		orderMap.put(Constants.SELECTED, orderList.get(0).getOrderId());
	}

	public static Map<String, String> getOrderMap(Order order) {
		orderMap.put(Constants.SELECTED, order.getOrderId());
		return orderMap;
	}

	public static Map<String, String> getOrderMap() {
		return orderMap;
	}

	@RequestMapping("/004")
	public String orderList(Map<String, Object> map) {
		map.put("orderList", orderService.getOrderList());
		return "OD/OD004";
	}

	@RequestMapping(value = "/001")
	public String createOrderPage(Map<String, Object> map) {
		map.put("order", new Order());
		initOrderPage(map, "001");
		return "OD/OD001";
	}

	public void initOrderPage(Map<String, Object> map, String type) {
		Map<String, String> initCustomerMap = new HashMap();
		Calendar calendar = Calendar.getInstance();
		Date orderDate = null;
		if ("001".equals(type)) {
			initCustomerMap = CustomerController.getCustomerMap();
		} else if ("002".equals(type)) {
			Order order = (Order) map.get("order");
			initCustomerMap = CustomerController.getCustomerMap(order.getCustomer());
			orderDate = order.getOrderDate();
		}
		if (orderDate == null) orderDate = calendar.getTime();
		String customerId = initCustomerMap.get(Constants.SELECTED);
		String customerName = initCustomerMap.get(customerId);
		map.put("customerId", customerId);
		map.put("customerName", customerName);
		map.put("orderDate", Constants.sdf.format(orderDate));
	}

	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String saveOrder(@ModelAttribute("order") Order order, HttpServletRequest request) {
		Customer customer = CustomerService.getInstance().loadCustomer(request.getParameter("customerId"));
		order.setCustomer(customer);
		String orderDate = (String) request.getParameter("order_date");
		try {
			if (orderDate != null) order.setOrderDate(Constants.sdf.parse(orderDate));
		} catch (ParseException pe) {
			logger.debug("ParseException in saving new order: "+pe.getMessage());
			order.setOrderDate(new Date());
		}
		orderService.createOrder(order);
		CustomerController.initCustomerMap();
		return "redirect:/OD/004.html";
	}

	@RequestMapping(value = "/002/{orderId}")
	public String updateOrderPage(@PathVariable("orderId") String orderId, Map<String, Object> map) {
		Order order = orderService.loadOrder(orderId);
		map.put("order", order);
		initOrderPage(map, "002");
		return "OD/OD002";
	}

	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public String updateOrder(@ModelAttribute("order") Order order, HttpServletRequest request) {
		Customer customer = CustomerService.getInstance().loadCustomer(request.getParameter("customerId"));
		order.setCustomer(customer);
		String orderDate = (String) request.getParameter("order_date");
		try {
			if (orderDate != null) order.setOrderDate(Constants.sdf.parse(orderDate));
		} catch (ParseException pe) {
			logger.debug("ParseException in updating existing order: "+pe.getMessage());
			order.setOrderDate(new Date());
		}
		orderService.updateOrder(order);
		return "redirect:/OD/004.html";
	}

	@RequestMapping("/003/{orderId}")
	public String deleteOrder(@PathVariable("orderId") String orderId) {
		orderService.removeOrder(orderId);
		return "redirect:/OD/004.html";
	}

	@RequestMapping(value="/004/orderList", method = RequestMethod.GET)
	public @ResponseBody Map jsonOrderList() {
		Map resultMap = new HashMap();
		List<Map> entityMapList = new ArrayList<Map>();
		List<Order> entityList = orderService.getOrderList();
		for (Order entity : entityList) {
			Map<String, String> entityMap = new HashMap<String, String>();
			
			entityMap.put("order_id", entity.getOrderId());
			entityMap.put("customer_name", entity.getCustomer().getCustomerName());
			entityMap.put("customer_id", entity.getCustomer().getCustomerId());
			entityMap.put("order_number", entity.getOrderNumber());
			entityMap.put("order_price", Double.toString(entity.getOrderPrice()));
			entityMap.put("order_date", entity.getOrderDate().toString());
			
			entityMap.put(Constants.CREATED_DATE, entity.getCreatedDate().toString());
			entityMap.put(Constants.LAST_MODIFIED_DATE, entity.getLastModifiedDate().toString());
			entityMapList.add(entityMap);
		}
		resultMap.put("success", true);
		resultMap.put("stats", entityMapList);
		return resultMap;
	}

	@RequestMapping(value="/004/setOrder", method = RequestMethod.POST)
	public @ResponseBody Map setOrderMap(HttpServletRequest request) {
		orderMap.put(Constants.SELECTED, request.getParameter("value"));
		return orderMap;
	}
	
	@RequestMapping(value="update", method = RequestMethod.PUT)
	public @ResponseBody Map update(@RequestBody Map requestMap) {
		String order_id = requestMap.get("order_id").toString();
		boolean isBookingDeleted = bookingService.deleteBookingByOrderId(order_id);
		if (isBookingDeleted) {
			orderService.removeOrder(order_id);
		}
		Order entity = new Order();
		Customer customer = CustomerService.getInstance().loadCustomer(requestMap.get("customer_id").toString());
		entity.setCustomer(customer);
		//entity.setOrderId(requestMap.get("order_id")!=null?requestMap.get("order_id").toString():"");
		entity.setOrderNumber(requestMap.get("order_number")!=null?requestMap.get("order_number").toString():"");
		entity.setOrderPrice(requestMap.get("order_price")!=null?Double.parseDouble(requestMap.get("order_price").toString()):0);
		entity.setOrderDate(new Date());
		String orderId = orderService.createOrder(entity);
		Map resultMap = new HashMap();
		resultMap.put("success", true);
		resultMap.put("orderId", orderId);
		return resultMap;
	}
	
	@RequestMapping(value="create", method = RequestMethod.POST)
	public @ResponseBody Map create(@RequestBody Map requestMap) {
		Order entity = new Order();
		Customer customer = CustomerService.getInstance().loadCustomer(requestMap.get("customer_id").toString());
		entity.setCustomer(customer);
		entity.setOrderNumber(requestMap.get("order_number")!=null?requestMap.get("order_number").toString():"");
		entity.setOrderPrice(requestMap.get("order_price")!=null?Double.parseDouble(requestMap.get("order_price").toString()):0);
		entity.setOrderDate(new Date());
		String orderId = orderService.createOrder(entity);
		Map resultMap = new HashMap();
		resultMap.put("success", true);
		resultMap.put("orderId", orderId);
		return resultMap;
	}

	@RequestMapping(value="delete", method = RequestMethod.DELETE)
	public @ResponseBody Map delete(@RequestBody Map requestMap) {
		String order_id = requestMap.get("order_id").toString();
		boolean isBookingDeleted = bookingService.deleteBookingByOrderId(order_id);
		Map resultMap = new HashMap();
		if (isBookingDeleted) {
			orderService.removeOrder(order_id);
			resultMap.put("success", true);
		}
		return resultMap;
	}

	@RequestMapping(method = RequestMethod.GET , value = "pdf")
	public ModelAndView generatePdfReport(ModelAndView modelAndView) {
		logger.debug("--------------generate PDF report----------");
		Map<String, Object> dataSourceMap = new HashMap<String, Object>();
		Map<String, Object> parameterMap = new HashMap<String, Object>();

		parameterMap.put(Constants.ORDER_NO, "Order Number 123");
		parameterMap.put(Constants.CUSTOMER_NO, "Customer Number 123");
		parameterMap.put(Constants.CUSTOMER_NAME, "Customer Name 123");
		parameterMap.put(Constants.CUSTOMER_ADDRESS, "Customer Address 123");
		parameterMap.put(Constants.CUSTOMER_CONTACT, "Customer Contact 123");
		parameterMap.put(Constants.CUSTOMER_PHONE, "Customer Phone 123");
		parameterMap.put(Constants.ORDER_DATE, "Order Date 123");
		parameterMap.put(Constants.DELIVERY_DATE, "Delivery Date 123");
		parameterMap.put(Constants.SALES_NAME, "Sales Name 123");
		List parameterList = new ArrayList();
		parameterList.add(parameterMap);

		//JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource((Collection) parameterMap);
		JRDataSource dataSource = new JRMapCollectionDataSource(parameterList);
		dataSourceMap.put("datasource", dataSource);
		modelAndView = new ModelAndView("pdfReport", dataSourceMap);
		return modelAndView;
	}

	@RequestMapping(method = RequestMethod.GET , value = "xls")
	public ModelAndView generateExcelReport(ModelAndView modelAndView) {
		logger.debug("--------------generate Excel report----------");
		Map<String, Object> dataSourceMap = new HashMap<String, Object>();
		Map<String, Object> parameterMap = new HashMap<String, Object>();

		parameterMap.put(Constants.ORDER_NO, "Order Number 123");
		parameterMap.put(Constants.CUSTOMER_NO, "Customer Number 123");
		parameterMap.put(Constants.CUSTOMER_NAME, "Customer Name 123");
		parameterMap.put(Constants.CUSTOMER_ADDRESS, "Customer Address 123");
		parameterMap.put(Constants.CUSTOMER_CONTACT, "Customer Contact 123");
		parameterMap.put(Constants.CUSTOMER_PHONE, "Customer Phone 123");
		parameterMap.put(Constants.ORDER_DATE, "Order Date 123");
		parameterMap.put(Constants.DELIVERY_DATE, "Delivery Date 123");
		parameterMap.put(Constants.SALES_NAME, "Sales Name 123");
		List parameterList = new ArrayList();
		parameterList.add(parameterMap);

		//JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource((Collection) parameterMap);
		JRDataSource dataSource = new JRMapCollectionDataSource(parameterList);
		dataSourceMap.put("datasource", dataSource);
		modelAndView = new ModelAndView("xlsReport", dataSourceMap);
		return modelAndView;
	 }
}