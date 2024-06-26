package com.drx.Project.controllers;

import com.drx.Project.model.*;
import com.drx.Project.properties.Params;
import com.drx.Project.service.ItemService;
import com.drx.Project.service.LogService;
import com.drx.Project.service.ServiceApi;
import com.drx.Project.service.UserService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/")
@AllArgsConstructor
public class ItemController {
    //variables for prometheus->grafana
    private final Counter itemsCounterAdded = Metrics.counter("my_items_added_counter");
    private final Counter itemsCounterRemoved = Metrics.counter("my_items_removed_counter");

    private final ItemService itemService;
    private final UserService userService;
    private final LogService logService;

    private ServiceApi serviceApi;
    //static variables - configuration processor
    private Params param;

    //method for display logging page
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    //method for main page
    @GetMapping
    public String showMainPage(Model model, Principal principal) {
        model.addAttribute("username", getUserFullName(principal));
        try{
            ZenQuote[] zenQuote = serviceApi.getZenQuote(param.getQUOTE_URL());
            if (zenQuote.length > 0) {
                model.addAttribute("quote", zenQuote[0]);
            }
        } catch (RestClientException e){
            model.addAttribute("quote", new ZenQuote(null, ""));
            model.addAttribute("error", "Unable to fetch quote. Please try again later.");
        }
        return "main";
    }

    //method for itemList page
    @GetMapping("/itemList")
    public String showItemList(Model model, Principal principal) {
        List<Item> items = itemService.getAllItems();
        model.addAttribute("items", items);
        //Add username to html
        model.addAttribute("username", getUserFullName(principal));
        return "items/itemList";
    }

    //method to get creating page
    @GetMapping("/items/create")
    public String showCreatePage(Model model, Principal principal) {
        model.addAttribute("itemDto", new ItemDto());
        //Add user full name to html page
        model.addAttribute("username", getUserFullName(principal));
        return "items/createItem";
    }

    //method for create Item and save to Log
    @PostMapping("/items/create")
    public String createItem(@Valid @ModelAttribute ItemDto itemDto,
                             BindingResult result,
                             Principal principal) {
        if (itemDto.getImageFile().isEmpty()) {
            result.addError(
                    new FieldError("itemDto", "imageFile", "The image file is required"));
        }
        if (result.hasErrors()) {
            return "items/createItem";
        }
        String storageFileName = saveImage(itemDto.getImageFile());
        if (storageFileName == null) {
            result.addError(
                    new FieldError("itemDto", "imageFile", "Error saving image file"));
            return "items/createItem";
        }

        Item item = convertToItem(itemDto, storageFileName);
        String description = descriptionOnSave(principal, item);
        itemService.saveToDb(item, description);
        saveLog(principal, item, description);
        itemsCounterAdded.increment();
        return "redirect:/itemList";
    }

    //method for get Item info
    @GetMapping("/items/info")
    public String showInfoPage(Model model, @RequestParam int id, Principal principal) {
        model.addAttribute("username", getUserFullName(principal));
        List<MyLog> myLogList = logService.getItemLogsById(id);
        model.addAttribute("itemLog", myLogList);
        Optional<Item> optionalItem = itemService.getById(id);
        if (optionalItem.isEmpty()) {
            return "redirect:/itemList";
        }
        Item item = optionalItem.get();
        model.addAttribute("item", item);
        return "items/infoItem";
    }

    //method to get edit page
    @GetMapping("/items/edit")
    public String showEditPage(Model model, @RequestParam int id, Principal principal) {
        model.addAttribute("username", getUserFullName(principal));
        Optional<Item> optionalItem = itemService.getById(id);
        if (optionalItem.isEmpty()) {
            return "redirect:/itemList";
        }
        Item item = optionalItem.get();
        model.addAttribute("item", item);
        model.addAttribute("itemDto", convertToItemDto(item));
        return "items/editItem";
    }

    //method for edit Item and save changes to Log
    @PostMapping("/items/edit")
    public String updateProduct(
            @RequestParam int id,
            @Valid @ModelAttribute ItemDto itemDto,
            BindingResult result,
            Principal principal,
            Model model
    ) {
        Optional<Item> optionalItem = itemService.getById(id);
        if (optionalItem.isEmpty()) {
            return "redirect:/itemList";
        }
        Item item = optionalItem.get();
        model.addAttribute("item", item);
        if (result.hasErrors()) {
            return "items/editItem";
        }
        if (!itemDto.getImageFile().isEmpty()) {
            deleteOldImage(item.getImageFileName());
            String storageFileName = saveImage(itemDto.getImageFile());
            if (storageFileName == null) {
                result.addError(
                        new FieldError("itemDto", "imageFile", "Error saving image file"));
                return "items/editItem";
            }
            item.setImageFileName(storageFileName);
        }
        String description = descriptionOnEdit(principal, item, itemDto);
        convertToItem(item, itemDto);
        itemService.saveToDb(item, description);
        if (!description.isEmpty()) {
            saveLog(principal, item, description);
        }
        return "redirect:/itemList";
    }

    //method for deleting Item
    @GetMapping("/items/delete")
    public String deleteProduct(@RequestParam int id, Principal principal) {
        Optional<Item> optionalItem = itemService.getById(id);

        if (optionalItem.isPresent()) {
            Item item = optionalItem.get();
            deleteOldImage(item.getImageFileName());
            itemService.deleteById(id, descriptionOnDelete(principal, item));
            itemsCounterRemoved.increment();
        }
        return "redirect:/itemList";
    }

    //method for deleting old image
    private void deleteOldImage(String imageFileName) {
        Path oldImagePath = Paths.get( param.getUPLOAD_DIR_IMG() + imageFileName);

        try {
            Files.deleteIfExists(oldImagePath);
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
    }

    //method for saving image
    private String saveImage(MultipartFile image) {
        String formattedDate = dateTime();
        String storageFileName = formattedDate + "_" + image.getOriginalFilename();
        try {
            Path uploadPath = Paths.get(param.getUPLOAD_DIR_IMG());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, Paths.get(param.getUPLOAD_DIR_IMG() + storageFileName)
                        , StandardCopyOption.REPLACE_EXISTING);
            }
            return storageFileName;
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            return null;
        }
    }

    //Saving object(Mylog) to database
    private void saveLog(Principal principal, Item item, String description) {
        MyLog myLog = new MyLog();
        myLog.setDescription(description);
        myLog.setItem(item);
        myLog.setUser(userService.getUserByUsername(principal.getName()).get());
        myLog.setCreatedAt(dateTime());
        logService.saveLogToDb(myLog);
    }

    //Description when saving the Item
    private String descriptionOnSave(Principal principal, Item item) {
        return getUserFullName(principal)
                + " added " + item.getName()
                + ", Status= " + item.getStatus()
                + ", Manufacturer= " + item.getManufacturer()
                + ", Category= " + item.getCategory()
                + ", Department= " + item.getDepartment()
                + ", Model= " + item.getModel()
                + ", S/N= " + item.getSerialNumber()
                + ", PO= " + item.getProductOrder()
                + ", Inv. nr.= " + item.getInventoryNumber()
                + ", Desc: " + item.getDescription();
    }

    //adding description if something is changing
    private String descriptionOnEdit(Principal principal, Item item, ItemDto itemDto) {
        StringBuilder description = new StringBuilder();
        //Save name, in case that it could be redefined
        String itemName = item.getName();

        if (!item.getName().equals(itemDto.getName())) {
            description.append(" Name to= ").append(itemDto.getName()).append(",");
        }
        if (!item.getStatus().equals(itemDto.getStatus())) {
            description.append(" Status to= ").append(itemDto.getStatus()).append(",");
        }
        if (!item.getManufacturer().equals(itemDto.getManufacturer())) {
            description.append(" Manufacturer to= ").append(itemDto.getManufacturer()).append(",");
        }
        if (!item.getCategory().equals(itemDto.getCategory())) {
            description.append(" Category to= ").append(itemDto.getCategory()).append(",");
        }
        if (!item.getDepartment().equals(itemDto.getDepartment())) {
            description.append(" Department to= ").append(itemDto.getDepartment()).append(",");
        }
        if (item.getModel() != null && !item.getModel().equals(itemDto.getModel())) {
            description.append(" Model to= ").append(itemDto.getModel()).append(",");
        }
        if (!item.getSerialNumber().equals(itemDto.getSerialNumber())) {
            description.append(" S/N to= ").append(itemDto.getSerialNumber()).append(",");
        }
        if (item.getProductOrder() != null && !item.getProductOrder().equals(itemDto.getProductOrder())) {
            description.append(" PO to= ").append(itemDto.getProductOrder()).append(",");
        }
        if (item.getInventoryNumber() != null && !item.getInventoryNumber().equals(itemDto.getInventoryNumber())) {
            description.append(" Inv. nr. to= ").append(itemDto.getInventoryNumber()).append(",");
        }
        if (!item.getDescription().equals(itemDto.getDescription())) {
            description.append(" Description to= ").append(itemDto.getDescription()).append(",");
        }
        //Add name to the beginning of the line if changes have been made
        if (!description.isEmpty()) {
            description.insert(0, getUserFullName(principal) + " modified " + itemName);
            //modify last symbol to "." in case if ","
            description.setCharAt(description.length() - 1, '.');
        }
        return description.toString();
    }

    //description when deleting Item
    private String descriptionOnDelete(Principal principal, Item item) {
        return getUserFullName(principal) + " deleted " + item.getName();
    }

    //convert Item to ItemDto (get data from Item)
    private ItemDto convertToItemDto(Item item) {
        ItemDto itemDto = new ItemDto();
        itemDto.setName(item.getName());
        itemDto.setStatus(item.getStatus());
        itemDto.setManufacturer(item.getManufacturer());
        itemDto.setCategory(item.getCategory());
        itemDto.setDepartment(item.getDepartment());
        itemDto.setModel(item.getModel());
        itemDto.setSerialNumber(item.getSerialNumber());
        itemDto.setProductOrder(item.getProductOrder());
        itemDto.setInventoryNumber(item.getInventoryNumber());
        itemDto.setDescription(item.getDescription());
        return itemDto;
    }

    //convert ItemDto to Item (get data from ItemDto)
    private Item convertToItem(ItemDto itemDto, String storageFileName) {
        Item item = new Item();
        item.setName(itemDto.getName());
        item.setStatus(itemDto.getStatus());
        item.setManufacturer(itemDto.getManufacturer());
        item.setCategory(itemDto.getCategory());
        item.setDepartment(itemDto.getDepartment());
        item.setModel(itemDto.getModel());
        item.setSerialNumber(itemDto.getSerialNumber());
        item.setProductOrder(itemDto.getProductOrder());
        item.setInventoryNumber(itemDto.getInventoryNumber());
        item.setDescription(itemDto.getDescription());
        item.setImageFileName(storageFileName);
        item.setCreatedAt(dateTime());
        item.setModifiedAt(dateTime());
        return item;
    }

    //convert ItemDto to Item (get data from ItemDto)
    private void convertToItem(Item item, ItemDto itemDto) {
        item.setName(itemDto.getName());
        item.setStatus(itemDto.getStatus());
        item.setManufacturer(itemDto.getManufacturer());
        item.setCategory(itemDto.getCategory());
        item.setDepartment(itemDto.getDepartment());
        item.setModel(itemDto.getModel());
        item.setSerialNumber(itemDto.getSerialNumber());
        item.setProductOrder(itemDto.getProductOrder());
        item.setInventoryNumber(itemDto.getInventoryNumber());
        item.setDescription(itemDto.getDescription());
        item.setModifiedAt(dateTime());
    }

    //Date and time
    private String dateTime() {
        LocalDateTime createdAt = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH∶mm∶ss");
        return createdAt.format(formatter);
    }
    //Get user full name
    private String getUserFullName(Principal principal) {
        User user = userService.getUserByUsername(principal.getName()).get();
        return user.getFullName();
    }


}

