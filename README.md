# SkillBridge - Custom Skill & Exchange Edition

This is a new SkillBridge project based on the existing project, with the original frontend styling preserved and the broken CSS escape characters removed.

## New skill functionality

### Add Skill / My Skills
- Programming is available as a category.
- Development, Programming, Design, Data, Database, Business and Communication are available.
- Selecting Other reveals a custom category input.
- Custom categories are saved normally.

### Skill Exchange
- Students can select a skill from My Skills to offer.
- Students can type a new skill to offer; it is automatically added to My Skills.
- Users can select a skill from the Skill Library to learn.
- Users can type a custom skill they want to learn.
- Teachers can select their own published skills or type a custom offered skill.
- Only the owner of an offered skill can use it as their offered skill.
- Exchange requests can still be accepted/rejected by the recipient.

## Frontend

```text
cd frontend
npm install
npm run dev
```

Frontend: http://localhost:5173

## Backend

```text
cd backend
mvn spring-boot:run
```

Backend: http://localhost:8080

## CSS validation

`frontend/src/styles.css` was checked after creation:
- no literal `\\n` escape sequences remain
- matching `{}` braces
- matching quote counts
- parsed successfully with a CSS parser with zero stylesheet parse errors

The original SkillBridge class names and visual design were retained instead of replacing the stylesheet with a different design system.
