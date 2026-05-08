import { Router } from "express";
import jwt from "jsonwebtoken";
import { z } from "zod";
import { env } from "../config/env";
import { pool } from "../db/mysql";
import { ApiError } from "../utils/ApiError";

const loginSchema = z.object({
  email: z.string().email(),
  senha: z.string().min(4)
});

export const authRouter = Router();

authRouter.post("/login", async (req, res, next) => {
  try {
    const { email, senha } = loginSchema.parse(req.body);
    // Login de demonstracao para testar o painel sem dependencia imediata de banco.
    if (email === env.DEMO_ADMIN_EMAIL && senha === env.DEMO_ADMIN_PASSWORD) {
      const demoUser = {
        id: 1,
        email: env.DEMO_ADMIN_EMAIL,
        nome: "Admin Demo",
        role: "ADMIN" as const
      };
      const demoToken = jwt.sign(demoUser, env.JWT_SECRET, {
        expiresIn: env.JWT_EXPIRES_IN as jwt.SignOptions["expiresIn"]
      });
      return res.json({ token: demoToken, user: demoUser });
    }

    const [rows] = await pool.query(
      "SELECT id, email, nome, role FROM users WHERE email = ? AND senha = ? LIMIT 1",
      [email, senha]
    );
    const users = rows as Array<{ id: number; email: string; nome: string; role: "ADMIN" | "USER" }>;
    if (!users.length) {
      throw new ApiError(401, "Credenciais invalidas.");
    }
    const user = users[0];
    const token = jwt.sign(user, env.JWT_SECRET, { expiresIn: env.JWT_EXPIRES_IN as jwt.SignOptions["expiresIn"] });
    return res.json({ token, user });
  } catch (error) {
    next(error);
  }
});
