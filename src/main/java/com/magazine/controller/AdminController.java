package com.magazine.controller;

import com.magazine.model.Magazine;
import com.magazine.service.MagazineService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "password123";
    private static final String SESSION_USER_KEY = "adminUser";

    private final MagazineService magazineService;

    public AdminController(MagazineService magazineService) {
        this.magazineService = magazineService;
    }

    /**
     * Show Admin Login Page
     */
    @GetMapping("/login")
    public String showLoginPage(HttpSession session, Model model) {
        if (session.getAttribute(SESSION_USER_KEY) != null) {
            return "redirect:/admin/dashboard";
        }
        return "admin-login";
    }

    /**
     * Handle Admin Login Authentication
     */
    @PostMapping("/login")
    public String processLogin(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               HttpSession session,
                               Model model) {
        if (ADMIN_USERNAME.equals(username.trim()) && ADMIN_PASSWORD.equals(password)) {
            session.setAttribute(SESSION_USER_KEY, ADMIN_USERNAME);
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("error", "Invalid username or password. Please try again.");
        return "admin-login";
    }

    /**
     * Show Admin Dashboard (Upload & Manage Magazines)
     */
    @GetMapping({"", "/", "/dashboard"})
    public String showDashboard(HttpSession session, Model model) {
        if (session.getAttribute(SESSION_USER_KEY) == null) {
            return "redirect:/admin/login";
        }

        List<Magazine> magazines = magazineService.getAllMagazines();
        model.addAttribute("magazines", magazines);
        model.addAttribute("totalMagazines", magazines.size());
        return "admin-dashboard";
    }

    /**
     * Handle Magazine Upload with Title, Description, and PDF (plus optional cover)
     */
    @PostMapping("/upload")
    public String uploadMagazine(@RequestParam("title") String title,
                                 @RequestParam("description") String description,
                                 @RequestParam(value = "category", required = false, defaultValue = "General") String category,
                                 @RequestParam("pdfFile") MultipartFile pdfFile,
                                 @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        if (session.getAttribute(SESSION_USER_KEY) == null) {
            return "redirect:/admin/login";
        }

        // Form validation
        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Magazine title is required.");
            return "redirect:/admin/dashboard";
        }
        if (description == null || description.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Magazine description is required.");
            return "redirect:/admin/dashboard";
        }
        if (pdfFile == null || pdfFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid PDF file to upload.");
            return "redirect:/admin/dashboard";
        }

        try {
            magazineService.saveMagazine(title, description, category, pdfFile, coverImage);
            redirectAttributes.addFlashAttribute("successMessage", "Magazine '" + title + "' uploaded successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "File upload failed: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    /**
     * Delete a Magazine
     */
    @PostMapping("/delete/{id}")
    public String deleteMagazine(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (session.getAttribute(SESSION_USER_KEY) == null) {
            return "redirect:/admin/login";
        }

        boolean deleted = magazineService.deleteMagazine(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("successMessage", "Magazine deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Magazine not found.");
        }
        return "redirect:/admin/dashboard";
    }

    /**
     * Admin Logout
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(SESSION_USER_KEY);
        session.invalidate();
        return "redirect:/admin/login?logout=true";
    }
}
